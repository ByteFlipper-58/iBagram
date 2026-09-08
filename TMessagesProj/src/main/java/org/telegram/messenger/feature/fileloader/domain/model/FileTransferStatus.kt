package org.telegram.messenger.feature.fileloader.domain.model

enum class FileTransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED
}
