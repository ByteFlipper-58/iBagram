package org.telegram.messenger.feature.media.fileloader.data.mapper

import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferModel
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferStatus
import org.telegram.messenger.feature.media.fileloader.domain.model.FileTransferType

object FileTransferMapper {

    fun toDownloadModel(
        fileName: String,
        transferredBytes: Long,
        totalBytes: Long,
        status: FileTransferStatus = FileTransferStatus.IN_PROGRESS,
        filePath: String? = null,
        errorMessage: String? = null
    ): FileTransferModel {
        val safeTotal = if (totalBytes > 0) totalBytes else 0L
        val progress = if (safeTotal > 0) {
            (transferredBytes.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
        } else if (status == FileTransferStatus.COMPLETED) {
            1f
        } else {
            0f
        }

        return FileTransferModel(
            id = fileName,
            name = fileName,
            type = FileTransferType.DOWNLOAD,
            status = status,
            transferredBytes = transferredBytes,
            totalBytes = safeTotal,
            progress = progress,
            filePath = filePath,
            mimeType = null,
            errorMessage = errorMessage
        )
    }

    fun toUploadModel(
        location: String,
        transferredBytes: Long,
        totalBytes: Long,
        status: FileTransferStatus = FileTransferStatus.IN_PROGRESS,
        filePath: String? = null,
        errorMessage: String? = null
    ): FileTransferModel {
        val safeTotal = if (totalBytes > 0) totalBytes else 0L
        val progress = if (safeTotal > 0) {
            (transferredBytes.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
        } else if (status == FileTransferStatus.COMPLETED) {
            1f
        } else {
            0f
        }

        val rawPath = filePath ?: location
        val name = rawPath.substringAfterLast('/').substringAfterLast('\\')

        return FileTransferModel(
            id = location,
            name = name,
            type = FileTransferType.UPLOAD,
            status = status,
            transferredBytes = transferredBytes,
            totalBytes = safeTotal,
            progress = progress,
            filePath = filePath,
            mimeType = null,
            errorMessage = errorMessage
        )
    }

    fun fromMessageObject(
        messageObject: MessageObject,
        status: FileTransferStatus = FileTransferStatus.IN_PROGRESS,
        transferredBytes: Long = 0L,
        filePath: String? = null
    ): FileTransferModel {
        val doc = messageObject.document
        val fileName = if (doc != null) {
            FileLoader.getDocumentFileName(doc).ifEmpty { messageObject.fileName ?: "file_${messageObject.id}" }
        } else {
            messageObject.fileName ?: "file_${messageObject.id}"
        }

        val totalBytes = messageObject.size.toLong().coerceAtLeast(0L)
        val progress = if (totalBytes > 0) {
            (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (status == FileTransferStatus.COMPLETED) {
            1f
        } else {
            0f
        }

        return FileTransferModel(
            id = fileName,
            name = fileName,
            type = FileTransferType.DOWNLOAD,
            status = status,
            transferredBytes = transferredBytes,
            totalBytes = totalBytes,
            progress = progress,
            filePath = filePath,
            mimeType = doc?.mime_type,
            errorMessage = null
        )
    }
}
