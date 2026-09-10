package org.telegram.messenger.feature.media.fileloader.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DownloadController
import org.telegram.messenger.FileLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.data.mapper.FileTransferMapper
import org.telegram.messenger.feature.media.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferStatus
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferType
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Clean data adapter implementing [FileLoaderRepository] backed by legacy [FileLoader],
 * [DownloadController], and [NotificationCenter].
 */
class LegacyFileLoaderRepository(
    private val currentAccount: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : FileLoaderRepository {

    private val fileLoader: FileLoader
        get() = FileLoader.getInstance(currentAccount)

    private val downloadController: DownloadController
        get() = DownloadController.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    private val transfersMap = ConcurrentHashMap<String, FileTransferModel>()
    private val _transfersFlow = MutableStateFlow<List<FileTransferModel>>(emptyList())

    private fun updateTransfer(id: String, update: (FileTransferModel?) -> FileTransferModel) {
        val updated = update(transfersMap[id])
        transfersMap[id] = updated
        _transfersFlow.value = transfersMap.values.toList()
    }

    private fun syncActiveDownloads() {
        val downloading = ArrayList(downloadController.downloadingFiles)
        for (msg in downloading) {
            val model = FileTransferMapper.fromMessageObject(
                messageObject = msg,
                status = FileTransferStatus.IN_PROGRESS
            )
            transfersMap.putIfAbsent(model.id, model)
        }

        val recent = ArrayList(downloadController.recentDownloadingFiles)
        for (msg in recent) {
            val model = FileTransferMapper.fromMessageObject(
                messageObject = msg,
                status = FileTransferStatus.COMPLETED
            )
            transfersMap.putIfAbsent(model.id, model)
        }

        _transfersFlow.value = transfersMap.values.toList()
    }

    override fun observeTransfers(): Flow<List<FileTransferModel>> = callbackFlow {
        fun emitCurrent() {
            syncActiveDownloads()
            trySend(_transfersFlow.value)
        }

        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, args ->
            when (id) {
                NotificationCenter.fileLoadProgressChanged -> {
                    val fileName = args?.getOrNull(0) as? String
                    val uploadedSize = (args?.getOrNull(1) as? Number)?.toLong() ?: 0L
                    val totalSize = (args?.getOrNull(2) as? Number)?.toLong() ?: 0L
                    if (fileName != null) {
                        updateTransfer(fileName) { existing ->
                            FileTransferMapper.toDownloadModel(
                                fileName = fileName,
                                transferredBytes = uploadedSize,
                                totalBytes = totalSize,
                                status = FileTransferStatus.IN_PROGRESS,
                                filePath = existing?.filePath
                            )
                        }
                    }
                }
                NotificationCenter.fileLoaded -> {
                    val fileName = args?.getOrNull(0) as? String
                    val finalFile = args?.getOrNull(1) as? File
                    if (fileName != null) {
                        val fileLength = finalFile?.length() ?: 0L
                        updateTransfer(fileName) { existing ->
                            FileTransferMapper.toDownloadModel(
                                fileName = fileName,
                                transferredBytes = existing?.totalBytes ?: fileLength,
                                totalBytes = existing?.totalBytes ?: fileLength,
                                status = FileTransferStatus.COMPLETED,
                                filePath = finalFile?.absolutePath ?: existing?.filePath
                            )
                        }
                    }
                }
                NotificationCenter.fileLoadFailed -> {
                    val fileName = args?.getOrNull(0) as? String
                    val reason = (args?.getOrNull(1) as? Number)?.toInt() ?: 0
                    if (fileName != null) {
                        updateTransfer(fileName) { existing ->
                            FileTransferMapper.toDownloadModel(
                                fileName = fileName,
                                transferredBytes = existing?.transferredBytes ?: 0L,
                                totalBytes = existing?.totalBytes ?: 0L,
                                status = FileTransferStatus.FAILED,
                                filePath = existing?.filePath,
                                errorMessage = "Load failed: code $reason"
                            )
                        }
                    }
                }
                NotificationCenter.fileUploadProgressChanged -> {
                    val location = args?.getOrNull(0) as? String
                    val uploadedSize = (args?.getOrNull(1) as? Number)?.toLong() ?: 0L
                    val totalSize = (args?.getOrNull(2) as? Number)?.toLong() ?: 0L
                    if (location != null) {
                        updateTransfer(location) { existing ->
                            FileTransferMapper.toUploadModel(
                                location = location,
                                transferredBytes = uploadedSize,
                                totalBytes = totalSize,
                                status = FileTransferStatus.IN_PROGRESS,
                                filePath = existing?.filePath ?: location
                            )
                        }
                    }
                }
                NotificationCenter.fileUploaded -> {
                    val location = args?.getOrNull(0) as? String
                    if (location != null) {
                        updateTransfer(location) { existing ->
                            FileTransferMapper.toUploadModel(
                                location = location,
                                transferredBytes = existing?.totalBytes ?: 0L,
                                totalBytes = existing?.totalBytes ?: 0L,
                                status = FileTransferStatus.COMPLETED,
                                filePath = existing?.filePath ?: location
                            )
                        }
                    }
                }
                NotificationCenter.fileUploadFailed -> {
                    val location = args?.getOrNull(0) as? String
                    if (location != null) {
                        updateTransfer(location) { existing ->
                            FileTransferMapper.toUploadModel(
                                location = location,
                                transferredBytes = existing?.transferredBytes ?: 0L,
                                totalBytes = existing?.totalBytes ?: 0L,
                                status = FileTransferStatus.FAILED,
                                filePath = existing?.filePath ?: location,
                                errorMessage = "Upload failed"
                            )
                        }
                    }
                }
                NotificationCenter.onDownloadingFilesChanged -> {
                    syncActiveDownloads()
                }
            }
            trySend(_transfersFlow.value)
        }

        notificationCenter.addObserver(delegate, NotificationCenter.fileLoadProgressChanged)
        notificationCenter.addObserver(delegate, NotificationCenter.fileLoaded)
        notificationCenter.addObserver(delegate, NotificationCenter.fileLoadFailed)
        notificationCenter.addObserver(delegate, NotificationCenter.fileUploadProgressChanged)
        notificationCenter.addObserver(delegate, NotificationCenter.fileUploaded)
        notificationCenter.addObserver(delegate, NotificationCenter.fileUploadFailed)
        notificationCenter.addObserver(delegate, NotificationCenter.onDownloadingFilesChanged)

        emitCurrent()

        awaitClose {
            notificationCenter.removeObserver(delegate, NotificationCenter.fileLoadProgressChanged)
            notificationCenter.removeObserver(delegate, NotificationCenter.fileLoaded)
            notificationCenter.removeObserver(delegate, NotificationCenter.fileLoadFailed)
            notificationCenter.removeObserver(delegate, NotificationCenter.fileUploadProgressChanged)
            notificationCenter.removeObserver(delegate, NotificationCenter.fileUploaded)
            notificationCenter.removeObserver(delegate, NotificationCenter.fileUploadFailed)
            notificationCenter.removeObserver(delegate, NotificationCenter.onDownloadingFilesChanged)
        }
    }.flowOn(dispatcher)

    override fun observeTransfer(fileId: String): Flow<FileTransferModel?> {
        return observeTransfers().map { list ->
            list.firstOrNull { it.id == fileId }
        }
    }

    override fun getActiveDownloads(): Flow<List<FileTransferModel>> {
        return observeTransfers().map { list ->
            list.filter { it.type == FileTransferType.DOWNLOAD && (it.status == FileTransferStatus.IN_PROGRESS || it.status == FileTransferStatus.PENDING) }
        }
    }

    override fun getRecentDownloads(): Flow<List<FileTransferModel>> {
        return observeTransfers().map { list ->
            list.filter { it.type == FileTransferType.DOWNLOAD && it.status == FileTransferStatus.COMPLETED }
        }
    }

    override suspend fun loadFile(request: FileDownloadRequest): Result<Unit> = withContext(dispatcher) {
        try {
            updateTransfer(request.fileName) {
                FileTransferMapper.toDownloadModel(
                    fileName = request.fileName,
                    transferredBytes = 0L,
                    totalBytes = request.size,
                    status = FileTransferStatus.PENDING
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to load file", e))
        }
    }

    override suspend fun cancelLoadFile(fileName: String): Result<Unit> = withContext(dispatcher) {
        try {
            fileLoader.cancelLoadFile(fileName)
            updateTransfer(fileName) { existing ->
                FileTransferMapper.toDownloadModel(
                    fileName = fileName,
                    transferredBytes = existing?.transferredBytes ?: 0L,
                    totalBytes = existing?.totalBytes ?: 0L,
                    status = FileTransferStatus.CANCELLED,
                    filePath = existing?.filePath
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel file load", e))
        }
    }

    override suspend fun cancelAllDownloads(): Result<Unit> = withContext(dispatcher) {
        try {
            fileLoader.cancelLoadAllFiles()
            transfersMap.forEach { (id, transfer) ->
                if (transfer.status == FileTransferStatus.IN_PROGRESS || transfer.status == FileTransferStatus.PENDING) {
                    transfersMap[id] = transfer.copy(status = FileTransferStatus.CANCELLED)
                }
            }
            _transfersFlow.value = transfersMap.values.toList()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel all downloads", e))
        }
    }

    override suspend fun uploadFile(request: FileUploadRequest): Result<String> = withContext(dispatcher) {
        try {
            val file = File(request.filePath)
            val size = if (file.exists()) file.length() else 0L
            updateTransfer(request.filePath) {
                FileTransferMapper.toUploadModel(
                    location = request.filePath,
                    transferredBytes = 0L,
                    totalBytes = size,
                    status = FileTransferStatus.PENDING,
                    filePath = request.filePath
                )
            }
            fileLoader.uploadFile(request.filePath, request.isEncrypted, request.isSmall, request.type)
            Result.Success(request.filePath)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to upload file", e))
        }
    }

    override suspend fun cancelFileUpload(location: String, isEncrypted: Boolean): Result<Unit> = withContext(dispatcher) {
        try {
            fileLoader.cancelFileUpload(location, isEncrypted)
            updateTransfer(location) { existing ->
                FileTransferMapper.toUploadModel(
                    location = location,
                    transferredBytes = existing?.transferredBytes ?: 0L,
                    totalBytes = existing?.totalBytes ?: 0L,
                    status = FileTransferStatus.CANCELLED,
                    filePath = existing?.filePath
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel file upload", e))
        }
    }

    override suspend fun isFileLoading(fileName: String): Boolean = withContext(dispatcher) {
        fileLoader.isLoadingFile(fileName)
    }

    override suspend fun getLocalFilePath(fileName: String): String? = withContext(dispatcher) {
        val existing = transfersMap[fileName]?.filePath
        if (existing != null && File(existing).exists()) {
            return@withContext existing
        }
        for (dirType in intArrayOf(
            FileLoader.MEDIA_DIR_CACHE,
            FileLoader.MEDIA_DIR_FILES,
            FileLoader.MEDIA_DIR_IMAGE,
            FileLoader.MEDIA_DIR_VIDEO,
            FileLoader.MEDIA_DIR_AUDIO
        )) {
            val dir = FileLoader.checkDirectory(dirType)
            if (dir != null) {
                val f = File(dir, fileName)
                if (f.exists()) {
                    return@withContext f.absolutePath
                }
            }
        }
        null
    }
}
