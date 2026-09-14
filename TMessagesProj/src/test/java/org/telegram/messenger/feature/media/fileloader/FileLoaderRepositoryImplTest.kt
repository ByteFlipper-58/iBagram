package org.telegram.messenger.feature.media.fileloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.data.datasource.FileLoaderLocalDataSource
import org.telegram.messenger.feature.media.fileloader.data.datasource.FileLoaderRemoteDataSource
import org.telegram.messenger.feature.media.fileloader.data.repository.FileLoaderRepositoryImpl
import org.telegram.messenger.feature.media.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferStatus
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferType
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest

class FileLoaderRepositoryImplTest {

    private lateinit var localDataSource: FileLoaderLocalDataSource
    private lateinit var remoteDataSource: FileLoaderRemoteDataSource
    private lateinit var repository: FileLoaderRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = FileLoaderLocalDataSource(0)
        remoteDataSource = FileLoaderRemoteDataSource(0)
        repository = FileLoaderRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun loadFile_updatesTransferAndReturnsSuccess() = runBlocking {
        val request = FileDownloadRequest(fileName = "video.mp4", size = 1024L)
        val result = repository.loadFile(request)

        assertTrue(result is Result.Success)
        val transfer = repository.observeTransfer("video.mp4").first()
        assertNotNull(transfer)
        assertEquals("video.mp4", transfer?.name)
        assertEquals(FileTransferType.DOWNLOAD, transfer?.type)
        assertEquals(FileTransferStatus.PENDING, transfer?.status)
    }

    @Test
    fun cancelLoadFile_updatesStatusToCancelled() = runBlocking {
        val request = FileDownloadRequest(fileName = "audio.mp3", size = 2048L)
        repository.loadFile(request)

        val cancelResult = repository.cancelLoadFile("audio.mp3")
        assertTrue(cancelResult is Result.Success)

        val transfer = repository.observeTransfer("audio.mp3").first()
        assertEquals(FileTransferStatus.CANCELLED, transfer?.status)
    }

    @Test
    fun cancelAllDownloads_cancelsOnlyActiveDownloads() = runBlocking {
        repository.loadFile(FileDownloadRequest(fileName = "file1.zip", size = 500L))
        repository.loadFile(FileDownloadRequest(fileName = "file2.zip", size = 800L))

        val cancelResult = repository.cancelAllDownloads()
        assertTrue(cancelResult is Result.Success)

        val active = repository.getActiveDownloads().first()
        assertTrue(active.isEmpty())

        val t1 = repository.observeTransfer("file1.zip").first()
        val t2 = repository.observeTransfer("file2.zip").first()
        assertEquals(FileTransferStatus.CANCELLED, t1?.status)
        assertEquals(FileTransferStatus.CANCELLED, t2?.status)
    }

    @Test
    fun uploadFile_updatesTransferAndReturnsSuccess() = runBlocking {
        val request = FileUploadRequest(filePath = "/storage/emulated/0/photo.jpg")
        val result = repository.uploadFile(request)

        assertTrue(result is Result.Success)
        assertEquals("/storage/emulated/0/photo.jpg", (result as Result.Success).data)

        val transfer = repository.observeTransfer("/storage/emulated/0/photo.jpg").first()
        assertNotNull(transfer)
        assertEquals("photo.jpg", transfer?.name)
        assertEquals(FileTransferType.UPLOAD, transfer?.type)
        assertEquals(FileTransferStatus.PENDING, transfer?.status)
    }

    @Test
    fun cancelFileUpload_updatesStatusToCancelled() = runBlocking {
        val path = "/storage/emulated/0/doc.pdf"
        repository.uploadFile(FileUploadRequest(filePath = path))

        val cancelResult = repository.cancelFileUpload(path, false)
        assertTrue(cancelResult is Result.Success)

        val transfer = repository.observeTransfer(path).first()
        assertEquals(FileTransferStatus.CANCELLED, transfer?.status)
    }

    @Test
    fun getLocalFilePath_resolvesCustomPath() = runBlocking {
        localDataSource.setCustomLocalPath("doc.pdf", "/custom/path/doc.pdf")
        val path = repository.getLocalFilePath("doc.pdf")
        assertEquals("/custom/path/doc.pdf", path)
    }

    @Test
    fun isFileLoading_returnsTrueForPendingAndInProgress() = runBlocking {
        assertFalse(repository.isFileLoading("test.doc"))
        repository.loadFile(FileDownloadRequest(fileName = "test.doc", size = 100L))
        assertTrue(repository.isFileLoading("test.doc"))
    }
}
