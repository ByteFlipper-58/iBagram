package org.telegram.messenger.feature.gallerysave.domain.model

/**
 * Pure domain model for per-dialog exceptions from default auto-save to gallery rules.
 */
data class GallerySaveDialogExceptionModel(
    val dialogId: Long,
    val savePhoto: Boolean = false,
    val saveVideo: Boolean = false,
    val limitVideoBytes: Long = GallerySaveTargetSettingsModel.DEFAULT_VIDEO_LIMIT_BYTES
) {
    val isEnabled: Boolean
        get() = savePhoto || saveVideo
}
