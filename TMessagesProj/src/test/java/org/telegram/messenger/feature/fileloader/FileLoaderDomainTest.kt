package org.telegram.messenger.feature.fileloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.fileloader.data.mapper.FileTransferMapper
import org.telegram.messenger.feature.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.fileloader.domain.model.FileTransferStatus
import org.telegram.messenger.feature.fileloader.domain.model.FileTransferType
import org.telegram.messenger.feature.fileloader.domain.model.FileUploadRequest
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelAllDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelFileUploadUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelLoadFileUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.GetActiveDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.GetRecentDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.LoadFileUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.ObserveTransferUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.ObserveTransfersUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.UploadFileUseCase
import org.telegram.messenger.feature.fileloader.presentation.FileLoaderEvent
import org.telegram.messenger.feature.fileloader.presentation.FileLoaderViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FileLoaderDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeFileLoaderRepository : FileLoaderRepository {
        val transfersMap = mutableMapOf<String, FileTransferModel>()
        val transfersFlow = MutableSharedFlow<List<FileTransferModel>>(replay = 1).apply {
            tryEmit(emptyList())
        }
        var shouldSucceed = true

        fun putTransfer(transfer: FileTransferModel) {
            transfersMap[transfer.id] = transfer
            transfersFlow.tryEmit(transfersMap.values.toList())
        }

        override fun observeTransfers(): Flow<List<FileTransferModel>> = transfersFlow.asSharedFlow()

        override fun observeTransfer(fileId: String): Flow<FileTransferModel?> {
            return observeTransfers().map { list -> list.firstOrNull { it.id == fileId } }
        }

        override fun getActiveDownloads(): Flow<List<FileTransferModel>> {
            return observeTransfers().map { list ->
                list.filter {
                    it.type == FileTransferType.DOWNLOAD && (it.status == FileTransferStatus.IN_PROGRESS || it.status == FileTransferStatus.PENDING)
                }
            }
        }

        override fun getRecentDownloads(): Flow<List<FileTransferModel>> {
            return observeTransfers().map { list ->
                list.filter {
                    it.type == FileTransferType.DOWNLOAD && it.status == FileTransferStatus.COMPLETED
                }
            }
        }

        override suspend fun loadFile(request: FileDownloadRequest): Result<Unit> {
            return if (shouldSucceed) {
                putTransfer(
                    FileTransferModel(
                        id = request.fileName,
                        name = request.fileName,
                        type = FileTransferType.DOWNLOAD,
                        status = FileTransferStatus.IN_PROGRESS,
                        transferredBytes = 0L,
                        totalBytes = request.size,
                        progress = 0f
                    )
                )
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Generic("Load file failed"))
            }
        }

        override suspend fun cancelLoadFile(fileName: String): Result<Unit> {
            return if (shouldSucceed) {
                val existing = transfersMap[fileName]
                if (existing != null) {
                    putTransfer(existing.copy(status = FileTransferStatus.CANCELLED))
                }
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Generic("Cancel load failed"))
            }
        }

        override suspend fun cancelAllDownloads(): Result<Unit> {
            return if (shouldSucceed) {
                transfersMap.forEach { (k, v) ->
                    if (v.type == FileTransferType.DOWNLOAD && (v.status == FileTransferStatus.IN_PROGRESS || v.status == FileTransferStatus.PENDING)) {
                        transfersMap[k] = v.copy(status = FileTransferStatus.CANCELLED)
                    }
                }
                transfersFlow.tryEmit(transfersMap.values.toList())
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Generic("Cancel all downloads failed"))
            }
        }

        override suspend fun uploadFile(request: FileUploadRequest): Result<String> {
            return if (shouldSucceed) {
                putTransfer(
                    FileTransferModel(
                        id = request.filePath,
                        name = request.filePath,
                        type = FileTransferType.UPLOAD,
                        status = FileTransferStatus.IN_PROGRESS,
                        transferredBytes = 0L,
                        totalBytes = 1000L,
                        progress = 0f,
                        filePath = request.filePath
                    )
                )
                Result.Success(request.filePath)
            } else {
                Result.Failure(AppError.Generic("Upload failed"))
            }
        }

        override suspend fun cancelFileUpload(location: String, isEncrypted: Boolean): Result<Unit> {
            return if (shouldSucceed) {
                val existing = transfersMap[location]
                if (existing != null) {
                    putTransfer(existing.copy(status = FileTransferStatus.CANCELLED))
                }
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Generic("Cancel upload failed"))
            }
        }

        override suspend fun isFileLoading(fileName: String): Boolean {
            val transfer = transfersMap[fileName]
            return transfer != null && transfer.status == FileTransferStatus.IN_PROGRESS
        }

        override suspend fun getLocalFilePath(fileName: String): String? {
            return transfersMap[fileName]?.filePath
        }
    }

    @Test
    fun `test ObserveTransfersUseCase emits transfers list`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = ObserveTransfersUseCase(repo)

        val item1 = FileTransferModel(
            id = "doc1.pdf",
            name = "doc1.pdf",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.IN_PROGRESS,
            transferredBytes = 500,
            totalBytes = 1000,
            progress = 0.5f
        )
        repo.putTransfer(item1)

        val results = mutableListOf<List<FileTransferModel>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().toList(results)
        }

        assertEquals(1, results.size)
        assertEquals("doc1.pdf", results[0][0].name)
        assertEquals(0.5f, results[0][0].progress, 0.001f)

        job.cancel()
    }

    @Test
    fun `test ObserveTransferUseCase returns specific transfer`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = ObserveTransferUseCase(repo)

        val item = FileTransferModel(
            id = "photo.jpg",
            name = "photo.jpg",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.COMPLETED,
            transferredBytes = 2048,
            totalBytes = 2048,
            progress = 1.0f,
            filePath = "/path/to/photo.jpg"
        )
        repo.putTransfer(item)

        val transfer = useCase("photo.jpg").first()
        assertNotNull(transfer)
        assertEquals("photo.jpg", transfer?.name)
        assertEquals(FileTransferStatus.COMPLETED, transfer?.status)

        val nonExistent = useCase("unknown.file").first()
        assertNull(nonExistent)
    }

    @Test
    fun `test GetActiveDownloadsUseCase filters active downloads only`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = GetActiveDownloadsUseCase(repo)

        val downloading = FileTransferModel(
            id = "file1.zip",
            name = "file1.zip",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.IN_PROGRESS,
            transferredBytes = 100,
            totalBytes = 200,
            progress = 0.5f
        )
        val completed = FileTransferModel(
            id = "file2.zip",
            name = "file2.zip",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.COMPLETED,
            transferredBytes = 200,
            totalBytes = 200,
            progress = 1.0f
        )
        val uploading = FileTransferModel(
            id = "file3.zip",
            name = "file3.zip",
            type = FileTransferType.UPLOAD,
            status = FileTransferStatus.IN_PROGRESS,
            transferredBytes = 50,
            totalBytes = 100,
            progress = 0.5f
        )
        repo.putTransfer(downloading)
        repo.putTransfer(completed)
        repo.putTransfer(uploading)

        val active = useCase().first()
        assertEquals(1, active.size)
        assertEquals("file1.zip", active[0].name)
    }

    @Test
    fun `test GetRecentDownloadsUseCase filters completed downloads only`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = GetRecentDownloadsUseCase(repo)

        val downloading = FileTransferModel(
            id = "file1.zip",
            name = "file1.zip",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.IN_PROGRESS,
            transferredBytes = 100,
            totalBytes = 200,
            progress = 0.5f
        )
        val completed = FileTransferModel(
            id = "file2.zip",
            name = "file2.zip",
            type = FileTransferType.DOWNLOAD,
            status = FileTransferStatus.COMPLETED,
            transferredBytes = 200,
            totalBytes = 200,
            progress = 1.0f
        )
        repo.putTransfer(downloading)
        repo.putTransfer(completed)

        val recent = useCase().first()
        assertEquals(1, recent.size)
        assertEquals("file2.zip", recent[0].name)
    }

    @Test
    fun `test LoadFileUseCase successfully triggers download`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = LoadFileUseCase(repo)

        val result = useCase(FileDownloadRequest("test_file.mp4", size = 5000L))
        assertTrue(result is Result.Success)
        val transfer = repo.transfersMap["test_file.mp4"]
        assertNotNull(transfer)
        assertEquals(FileTransferStatus.IN_PROGRESS, transfer?.status)
    }

    @Test
    fun `test CancelLoadFileUseCase cancels file download`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = CancelLoadFileUseCase(repo)

        repo.putTransfer(
            FileTransferModel(
                id = "test_file.mp4",
                name = "test_file.mp4",
                type = FileTransferType.DOWNLOAD,
                status = FileTransferStatus.IN_PROGRESS,
                transferredBytes = 100,
                totalBytes = 500,
                progress = 0.2f
            )
        )

        val result = useCase("test_file.mp4")
        assertTrue(result is Result.Success)
        assertEquals(FileTransferStatus.CANCELLED, repo.transfersMap["test_file.mp4"]?.status)
    }

    @Test
    fun `test CancelAllDownloadsUseCase cancels all active downloads`() = runTest {
        val repo = FakeFileLoaderRepository()
        val useCase = CancelAllDownloadsUseCase(repo)

        repo.putTransfer(
            FileTransferModel(
                id = "d1",
                name = "d1",
                type = FileTransferType.DOWNLOAD,
                status = FileTransferStatus.IN_PROGRESS,
                transferredBytes = 10,
                totalBytes = 100,
                progress = 0.1f
            )
        )
        repo.putTransfer(
            FileTransferModel(
                id = "d2",
                name = "d2",
                type = FileTransferType.DOWNLOAD,
                status = FileTransferStatus.PENDING,
                transferredBytes = 0,
                totalBytes = 100,
                progress = 0f
            )
        )

        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(FileTransferStatus.CANCELLED, repo.transfersMap["d1"]?.status)
        assertEquals(FileTransferStatus.CANCELLED, repo.transfersMap["d2"]?.status)
    }

    @Test
    fun `test UploadFileUseCase and CancelFileUploadUseCase`() = runTest {
        val repo = FakeFileLoaderRepository()
        val uploadUseCase = UploadFileUseCase(repo)
        val cancelUseCase = CancelFileUploadUseCase(repo)

        val uploadResult = uploadUseCase(FileUploadRequest("/sdcard/upload.jpg"))
        assertTrue(uploadResult is Result.Success)
        assertEquals("/sdcard/upload.jpg", (uploadResult as Result.Success).data)
        assertEquals(FileTransferStatus.IN_PROGRESS, repo.transfersMap["/sdcard/upload.jpg"]?.status)

        val cancelResult = cancelUseCase("/sdcard/upload.jpg", false)
        assertTrue(cancelResult is Result.Success)
        assertEquals(FileTransferStatus.CANCELLED, repo.transfersMap["/sdcard/upload.jpg"]?.status)
    }

    @Test
    fun `test FileTransferMapper progress calculations`() {
        val downloadModel = FileTransferMapper.toDownloadModel(
            fileName = "movie.mp4",
            transferredBytes = 250L,
            totalBytes = 1000L
        )
        assertEquals(0.25f, downloadModel.progress, 0.001f)
        assertEquals(FileTransferType.DOWNLOAD, downloadModel.type)
        assertEquals(FileTransferStatus.IN_PROGRESS, downloadModel.status)

        val completedModel = FileTransferMapper.toDownloadModel(
            fileName = "finished.doc",
            transferredBytes = 500L,
            totalBytes = 500L,
            status = FileTransferStatus.COMPLETED
        )
        assertEquals(1.0f, completedModel.progress, 0.001f)
        assertEquals(FileTransferStatus.COMPLETED, completedModel.status)

        val uploadModel = FileTransferMapper.toUploadModel(
            location = "/path/to/archive.tar",
            transferredBytes = 100L,
            totalBytes = 400L
        )
        assertEquals(0.25f, uploadModel.progress, 0.001f)
        assertEquals(FileTransferType.UPLOAD, uploadModel.type)
        assertEquals("archive.tar", uploadModel.name)
    }

    @Test
    fun `test FileLoaderViewModel collects transfers and handles events`() = runTest {
        val repo = FakeFileLoaderRepository()
        val activeUseCase = GetActiveDownloadsUseCase(repo)
        val recentUseCase = GetRecentDownloadsUseCase(repo)
        val observeUseCase = ObserveTransfersUseCase(repo)
        val loadUseCase = LoadFileUseCase(repo)
        val cancelLoadUseCase = CancelLoadFileUseCase(repo)
        val cancelAllUseCase = CancelAllDownloadsUseCase(repo)
        val uploadUseCase = UploadFileUseCase(repo)
        val cancelUploadUseCase = CancelFileUploadUseCase(repo)

        val vm = FileLoaderViewModel(
            observeTransfersUseCase = observeUseCase,
            getActiveDownloadsUseCase = activeUseCase,
            getRecentDownloadsUseCase = recentUseCase,
            loadFileUseCase = loadUseCase,
            cancelLoadFileUseCase = cancelLoadUseCase,
            cancelAllDownloadsUseCase = cancelAllUseCase,
            uploadFileUseCase = uploadUseCase,
            cancelFileUploadUseCase = cancelUploadUseCase
        )

        advanceUntilIdle()

        // Test LoadFile event
        vm.onEvent(FileLoaderEvent.LoadFile(FileDownloadRequest("song.mp3", size = 2000L)))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.activeTransfers.size)
        assertEquals("song.mp3", vm.uiState.value.activeTransfers[0].name)
        assertFalse(vm.uiState.value.isLoading)

        // Test SelectTransfer
        vm.onEvent(FileLoaderEvent.SelectTransfer("song.mp3"))
        assertEquals("song.mp3", vm.uiState.value.selectedTransfer?.name)

        vm.onEvent(FileLoaderEvent.SelectTransfer(null))
        assertNull(vm.uiState.value.selectedTransfer)

        // Test CancelLoad
        vm.onEvent(FileLoaderEvent.CancelLoad("song.mp3"))
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.activeTransfers.size)

        // Test DismissError
        vm.onEvent(FileLoaderEvent.DismissError)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `test AccountFeatureContainer wires fileloader correctly`() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.fileLoaderRepository)
        assertNotNull(container.observeTransfersUseCase)
        assertNotNull(container.observeTransferUseCase)
        assertNotNull(container.getActiveDownloadsUseCase)
        assertNotNull(container.getRecentDownloadsUseCase)
        assertNotNull(container.loadFileUseCase)
        assertNotNull(container.cancelLoadFileUseCase)
        assertNotNull(container.cancelAllDownloadsUseCase)
        assertNotNull(container.uploadFileUseCase)
        assertNotNull(container.cancelFileUploadUseCase)
        assertNotNull(container.fileLoaderViewModel)
    }
}
