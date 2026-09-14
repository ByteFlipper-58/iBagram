package org.telegram.messenger.feature.media.fileloader.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.fileloader.data.datasource.FileLoaderLocalDataSource
import org.telegram.messenger.feature.media.fileloader.data.datasource.FileLoaderRemoteDataSource
import org.telegram.messenger.feature.media.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferStatus
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferType
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository

class FileLoaderRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: FileLoaderLocalDataSource,
    private val remoteDataSource: FileLoaderRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileLoaderRepository {

    override fun observeTransfers(): Flow<List<FileTransferModel>> {
        return localDataSource.transfersFlow
    }

    override fun observeTransfer(fileId: String): Flow<FileTransferModel?> {
        return localDataSource.transfersFlow.map { list ->
            list.firstOrNull { it.id == fileId }
        }
    }

    override fun getActiveDownloads(): Flow<List<FileTransferModel>> {
        return localDataSource.transfersFlow.map { list ->
            list.filter {
                it.type == FileTransferType.DOWNLOAD &&
                    (it.status == FileTransferStatus.IN_PROGRESS || it.status == FileTransferStatus.PENDING)
            }
        }
    }

    override fun getRecentDownloads(): Flow<List<FileTransferModel>> {
        return localDataSource.transfersFlow.map { list ->
            list.filter {
                it.type == FileTransferType.DOWNLOAD &&
                    it.status == FileTransferStatus.COMPLETED
            }
        }
    }

    override suspend fun loadFile(request: FileDownloadRequest): Result<Unit> = withContext(dispatcher) {
        try {
            val initialModel = FileTransferModel(
                id = request.fileName,
                name = request.fileName,
                type = FileTransferType.DOWNLOAD,
                status = FileTransferStatus.PENDING,
                transferredBytes = 0L,
                totalBytes = request.size,
                progress = 0f
            )
            localDataSource.updateTransfer(initialModel)

            remoteDataSource.requestDownload(
                fileName = request.fileName,
                priority = request.priority,
                size = request.size,
                documentId = request.documentId
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to start file download", e))
        }
    }

    override suspend fun cancelLoadFile(fileName: String): Result<Unit> = withContext(dispatcher) {
        try {
            remoteDataSource.cancelDownload(fileName)
            val existing = localDataSource.getTransfer(fileName)
            if (existing != null) {
                localDataSource.updateTransfer(existing.copy(status = FileTransferStatus.CANCELLED))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel file download", e))
        }
    }

    override suspend fun cancelAllDownloads(): Result<Unit> = withContext(dispatcher) {
        try {
            remoteDataSource.cancelAllDownloads()
            val current = localDataSource.transfersFlow.value
            for (transfer in current) {
                if (transfer.type == FileTransferType.DOWNLOAD &&
                    (transfer.status == FileTransferStatus.IN_PROGRESS || transfer.status == FileTransferStatus.PENDING)) {
                    localDataSource.updateTransfer(transfer.copy(status = FileTransferStatus.CANCELLED))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel all downloads", e))
        }
    }

    override suspend fun uploadFile(request: FileUploadRequest): Result<String> = withContext(dispatcher) {
        try {
            val name = request.filePath.substringAfterLast('/', request.filePath.substringAfterLast('\\'))
            val model = FileTransferModel(
                id = request.filePath,
                name = name,
                type = FileTransferType.UPLOAD,
                status = FileTransferStatus.PENDING,
                transferredBytes = 0L,
                totalBytes = 0L,
                progress = 0f,
                filePath = request.filePath
            )
            localDataSource.updateTransfer(model)

            remoteDataSource.requestUpload(
                filePath = request.filePath,
                isEncrypted = request.isEncrypted,
                isSmall = request.isSmall,
                type = request.type
            )

            Result.Success(request.filePath)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to upload file", e))
        }
    }

    override suspend fun cancelFileUpload(location: String, isEncrypted: Boolean): Result<Unit> = withContext(dispatcher) {
        try {
            remoteDataSource.cancelUpload(location, isEncrypted)
            val existing = localDataSource.getTransfer(location)
            if (existing != null) {
                localDataSource.updateTransfer(existing.copy(status = FileTransferStatus.CANCELLED))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to cancel file upload", e))
        }
    }

    override suspend fun isFileLoading(fileName: String): Boolean = withContext(dispatcher) {
        val transfer = localDataSource.getTransfer(fileName)
        if (transfer != null && (transfer.status == FileTransferStatus.IN_PROGRESS || transfer.status == FileTransferStatus.PENDING)) {
            return@withContext true
        }
        remoteDataSource.isFileLoading(fileName)
    }

    override suspend fun getLocalFilePath(fileName: String): String? = withContext(dispatcher) {
        localDataSource.resolveLocalPath(fileName)
    }
}
