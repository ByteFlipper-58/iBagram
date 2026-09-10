package org.telegram.messenger.feature.media.photoviewer.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.media.photoviewer.domain.repository.PhotoViewerRepository

/**
 * ViewModel orchestrating PhotoViewer MVI events and StateFlow.
 */
class PhotoViewerViewModel(
    private val repository: PhotoViewerRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
) {

    private val _uiState = MutableStateFlow(PhotoViewerUiState.fromDomain(repository.getState()))
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeState().collect { domainState ->
                _uiState.value = PhotoViewerUiState.fromDomain(domainState)
            }
        }
    }

    fun onEvent(event: PhotoViewerEvent) {
        when (event) {
            is PhotoViewerEvent.Open -> {
                repository.open(event.items, event.initialIndex, event.selectType)
            }
            is PhotoViewerEvent.Close -> {
                repository.close()
            }
            is PhotoViewerEvent.Next -> {
                repository.next()
            }
            is PhotoViewerEvent.Previous -> {
                repository.previous()
            }
            is PhotoViewerEvent.SelectIndex -> {
                repository.navigateTo(event.index)
            }
            is PhotoViewerEvent.SetEditMode -> {
                repository.setEditMode(event.mode)
            }
            is PhotoViewerEvent.ToggleActionBar -> {
                repository.toggleActionBar()
            }
            is PhotoViewerEvent.ToggleCaption -> {
                repository.toggleCaptionExpanded()
            }
            is PhotoViewerEvent.UpdateTransform -> {
                repository.updateTransform(
                    scale = event.scale,
                    translationX = event.translationX,
                    translationY = event.translationY,
                    rotation = event.rotation
                )
            }
            is PhotoViewerEvent.ResetTransform -> {
                repository.resetTransform()
            }
            is PhotoViewerEvent.TogglePlayback -> {
                val currentPlaying = repository.getState().playbackState.isPlaying
                repository.updatePlayback(isPlaying = !currentPlaying)
            }
            is PhotoViewerEvent.SeekTo -> {
                repository.updatePlayback(positionMs = event.positionMs)
            }
            is PhotoViewerEvent.SetSpeed -> {
                repository.updatePlayback(speed = event.speed)
            }
            is PhotoViewerEvent.SetQuality -> {
                repository.updatePlayback(quality = event.quality)
            }
            is PhotoViewerEvent.ExecuteAction -> {
                repository.executeAction(event.action)
            }
        }
        _uiState.value = PhotoViewerUiState.fromDomain(repository.getState())
    }

    fun clear() {
        scope.cancel()
    }
}
