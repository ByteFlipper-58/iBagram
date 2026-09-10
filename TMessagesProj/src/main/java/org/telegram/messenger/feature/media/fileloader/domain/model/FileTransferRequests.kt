package org.telegram.messenger.feature.media.fileloader.domain.model

data class FileDownloadRequest(
    val fileName: String,
    val priority: Int = 1,
    val size: Long = 0L,
    val documentId: Long? = null
)

data class FileUploadRequest(
    val filePath: String,
    val isEncrypted: Boolean = false,
    val isSmall: Boolean = false,
    val type: Int = 0
)
