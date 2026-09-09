package org.telegram.messenger.feature.gallerysave.domain.model

/**
 * Pure domain model for auto-save to gallery settings per peer category (Private chats, Groups, Channels).
 */
data class GallerySaveTargetSettingsModel(
    val peerType: GallerySavePeerType,
    val savePhoto: Boolean = false,
    val saveVideo: Boolean = false,
    val limitVideoBytes: Long = DEFAULT_VIDEO_LIMIT_BYTES
) {
    val isEnabled: Boolean
        get() = savePhoto || saveVideo

    companion object {
        const val DEFAULT_VIDEO_LIMIT_BYTES = 100 * 1024 * 1024L // 100 MB
        const val MAX_VIDEO_LIMIT_BYTES = 4000L * 1024 * 1024L // 4 GB
    }
}
