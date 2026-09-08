package org.telegram.messenger.feature.fileloader.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.fileloader.domain.model.FileUploadRequest

interface FileLoaderRepository {
    fun observeTransfers(): Flow<List<FileTransferModel>>
    fun observeTransfer(fileId: String): Flow<FileTransferModel?>
    fun getActiveDownloads(): Flow<List<FileTransferModel>>
    fun getRecentDownloads(): Flow<List<FileTransferModel>>
    suspend fun loadFile(request: FileDownloadRequest): Result<Unit>
    suspend fun cancelLoadFile(fileName: String): Result<Unit>
    suspend fun cancelAllDownloads(): Result<Unit>
    suspend fun uploadFile(request: FileUploadRequest): Result<String>
    suspend fun cancelFileUpload(location: String, isEncrypted: Boolean): Result<Unit>
    suspend fun isFileLoading(fileName: String): Boolean
    suspend fun getLocalFilePath(fileName: String): String?
}
