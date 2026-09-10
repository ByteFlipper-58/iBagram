package org.telegram.messenger.feature.media.mediadata.presentation

/**
 * One-off UI events for media and gallery interactions.
 */
sealed class MediaEvent {
    data class ShowToast(val message: String) : MediaEvent()
    data class ShowError(val message: String?) : MediaEvent()
    data class AlbumSelected(val albumId: Int) : MediaEvent()
    data class MediaSelected(val mediaId: Int) : MediaEvent()
}
