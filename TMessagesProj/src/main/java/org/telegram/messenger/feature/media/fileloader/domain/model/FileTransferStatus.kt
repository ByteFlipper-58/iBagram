package org.telegram.messenger.feature.media.fileloader.domain.model

enum class FileTransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED
}
