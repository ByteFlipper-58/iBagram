package org.telegram.messenger.feature.media.mediadata.presentation

import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel

/**
 * UI State for gallery / media screen.
 */
sealed class MediaUiState {
    object Loading : MediaUiState()

    data class Success(
        val albums: List<MediaAlbumModel> = emptyList(),
        val selectedAlbum: MediaAlbumModel? = null,
        val mediaItems: List<MediaItemModel> = emptyList(),
        val isLoadingMedia: Boolean = false
    ) : MediaUiState()

    data class Error(val message: String?) : MediaUiState()
}
