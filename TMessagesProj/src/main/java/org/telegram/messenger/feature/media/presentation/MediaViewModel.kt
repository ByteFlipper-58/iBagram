package org.telegram.messenger.feature.media.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.domain.usecase.GetAlbumMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetAllMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetMediaAlbumsUseCase
import org.telegram.messenger.feature.media.domain.usecase.ObserveMediaAlbumsUseCase

/**
 * ViewModel managing media albums, media browsing, and selection.
 */
class MediaViewModel(
    private val observeMediaAlbumsUseCase: ObserveMediaAlbumsUseCase,
    private val getMediaAlbumsUseCase: GetMediaAlbumsUseCase,
    private val getAlbumMediaUseCase: GetAlbumMediaUseCase,
    private val getAllMediaUseCase: GetAllMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _events = Channel<MediaEvent>(Channel.BUFFERED)
    val events: Flow<MediaEvent> = _events.receiveAsFlow()

    init {
        observeAlbums()
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            observeMediaAlbumsUseCase().collect { albums ->
                val current = _uiState.value
                val selected = if (current is MediaUiState.Success && current.selectedAlbum != null) {
                    albums.firstOrNull { it.id == current.selectedAlbum.id } ?: albums.firstOrNull()
                } else {
                    albums.firstOrNull()
                }
                val mediaItems = selected?.items ?: emptyList()
                _uiState.value = MediaUiState.Success(
                    albums = albums,
                    selectedAlbum = selected,
                    mediaItems = mediaItems,
                    isLoadingMedia = false
                )
            }
        }
    }

    fun selectAlbum(albumId: Int) {
        val current = _uiState.value as? MediaUiState.Success ?: return
        val album = current.albums.firstOrNull { it.id == albumId } ?: return

        _uiState.value = current.copy(
            selectedAlbum = album,
            isLoadingMedia = true
        )

        viewModelScope.launch {
            when (val result = getAlbumMediaUseCase(albumId)) {
                is Result.Success -> {
                    val updatedState = _uiState.value as? MediaUiState.Success ?: return@launch
                    _uiState.value = updatedState.copy(
                        mediaItems = result.data,
                        isLoadingMedia = false
                    )
                    _events.send(MediaEvent.AlbumSelected(albumId))
                }
                is Result.Failure -> {
                    val updatedState = _uiState.value as? MediaUiState.Success ?: return@launch
                    _uiState.value = updatedState.copy(isLoadingMedia = false)
                    _events.send(MediaEvent.ShowError(result.error.message))
                }
            }
        }
    }

    fun loadAllMedia() {
        val current = _uiState.value as? MediaUiState.Success
        _uiState.value = current?.copy(isLoadingMedia = true) ?: MediaUiState.Loading

        viewModelScope.launch {
            when (val result = getAllMediaUseCase()) {
                is Result.Success -> {
                    val existingAlbums = current?.albums ?: emptyList()
                    _uiState.value = MediaUiState.Success(
                        albums = existingAlbums,
                        selectedAlbum = null,
                        mediaItems = result.data,
                        isLoadingMedia = false
                    )
                }
                is Result.Failure -> {
                    if (current != null) {
                        _uiState.value = current.copy(isLoadingMedia = false)
                    } else {
                        _uiState.value = MediaUiState.Error(result.error.message)
                    }
                    _events.send(MediaEvent.ShowError(result.error.message))
                }
            }
        }
    }

    fun selectMedia(mediaId: Int) {
        viewModelScope.launch {
            _events.send(MediaEvent.MediaSelected(mediaId))
        }
    }
}
