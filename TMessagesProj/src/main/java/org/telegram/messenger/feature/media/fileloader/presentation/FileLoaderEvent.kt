package org.telegram.messenger.feature.media.fileloader.presentation

import org.telegram.messenger.feature.media.fileloader.domain.model.FileDownloadRequest
import org.telegram.messenger.feature.media.fileloader.domain.model.FileUploadRequest

sealed interface FileLoaderEvent {
    data class LoadFile(val request: FileDownloadRequest) : FileLoaderEvent
    data class CancelLoad(val fileName: String) : FileLoaderEvent
    object CancelAllDownloads : FileLoaderEvent
    data class UploadFile(val request: FileUploadRequest) : FileLoaderEvent
    data class CancelUpload(val location: String, val isEncrypted: Boolean = false) : FileLoaderEvent
    data class SelectTransfer(val fileId: String?) : FileLoaderEvent
    object DismissError : FileLoaderEvent
}
