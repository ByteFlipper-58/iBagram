package org.telegram.messenger.feature.media.fileloader.domain.model

data class FileTransferModel(
    val id: String,
    val name: String,
    val type: FileTransferType,
    val status: FileTransferStatus,
    val transferredBytes: Long,
    val totalBytes: Long,
    val progress: Float,
    val filePath: String? = null,
    val mimeType: String? = null,
    val errorMessage: String? = null
)
