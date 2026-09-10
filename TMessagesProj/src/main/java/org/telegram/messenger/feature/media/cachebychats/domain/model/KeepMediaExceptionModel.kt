package org.telegram.messenger.feature.media.cachebychats.domain.model

/**
 * Domain model describing a cache retention exception for a specific dialog.
 */
data class KeepMediaExceptionModel(
    val dialogId: Long,
    val type: CacheChatType,
    val duration: KeepMediaDuration
)
