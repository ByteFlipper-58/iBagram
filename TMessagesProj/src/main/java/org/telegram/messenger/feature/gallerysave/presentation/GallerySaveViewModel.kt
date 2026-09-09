package org.telegram.messenger.feature.gallerysave.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.usecase.GetGallerySaveConfigUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.GetGallerySaveSettingsUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.ObserveGallerySaveConfigUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.RemoveAllGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.RemoveGallerySaveExceptionUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.SetGallerySaveExceptionUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.SetGallerySaveVideoLimitUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.ToggleGallerySavePeerTypeUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.UpdateGallerySaveSettingsUseCase

/**
 * ViewModel managing auto-save to gallery rules, video size thresholds, and per-chat exceptions.
 */
class GallerySaveViewModel(
    private val observeGallerySaveConfigUseCase: ObserveGallerySaveConfigUseCase,
    private val getGallerySaveConfigUseCase: GetGallerySaveConfigUseCase,
    private val getGallerySaveSettingsUseCase: GetGallerySaveSettingsUseCase,
    private val updateGallerySaveSettingsUseCase: UpdateGallerySaveSettingsUseCase,
    private val toggleGallerySavePeerTypeUseCase: ToggleGallerySavePeerTypeUseCase,
    private val setGallerySaveVideoLimitUseCase: SetGallerySaveVideoLimitUseCase,
    private val setGallerySaveExceptionUseCase: SetGallerySaveExceptionUseCase,
    private val removeGallerySaveExceptionUseCase: RemoveGallerySaveExceptionUseCase,
    private val removeAllGallerySaveExceptionsUseCase: RemoveAllGallerySaveExceptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GallerySaveUiState(config = getGallerySaveConfigUseCase())
    )
    val uiState: StateFlow<GallerySaveUiState> = _uiState.asStateFlow()

    init {
        observeGallerySaveConfigUseCase()
            .onEach { config ->
                _uiState.update { it.copy(config = config) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GallerySaveEvent) {
        when (event) {
            is GallerySaveEvent.SelectPeerType -> selectPeerType(event.peerType)
            is GallerySaveEvent.TogglePhoto -> togglePhoto(event.peerType)
            is GallerySaveEvent.ToggleVideo -> toggleVideo(event.peerType)
            is GallerySaveEvent.TogglePeer -> togglePeer(event.peerType)
            is GallerySaveEvent.SetVideoLimit -> setVideoLimit(event.peerType, event.limitBytes)
            is GallerySaveEvent.SetException -> setException(event.peerType, event.exception)
            is GallerySaveEvent.RemoveException -> removeException(event.peerType, event.dialogId)
            is GallerySaveEvent.RemoveAllExceptions -> removeAllExceptions(event.peerType)
            is GallerySaveEvent.DismissMessages -> dismissMessages()
        }
    }

    private fun selectPeerType(peerType: GallerySavePeerType) {
        _uiState.update { it.copy(selectedPeerType = peerType) }
    }

    private fun togglePhoto(peerType: GallerySavePeerType) {
        viewModelScope.launch {
            val current = getGallerySaveSettingsUseCase(peerType)
            updateGallerySaveSettingsUseCase(current.copy(savePhoto = !current.savePhoto))
        }
    }

    private fun toggleVideo(peerType: GallerySavePeerType) {
        viewModelScope.launch {
            val current = getGallerySaveSettingsUseCase(peerType)
            updateGallerySaveSettingsUseCase(current.copy(saveVideo = !current.saveVideo))
        }
    }

    private fun togglePeer(peerType: GallerySavePeerType) {
        viewModelScope.launch {
            toggleGallerySavePeerTypeUseCase(peerType)
        }
    }

    private fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long) {
        viewModelScope.launch {
            setGallerySaveVideoLimitUseCase(peerType, limitBytes)
        }
    }

    private fun setException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        viewModelScope.launch {
            setGallerySaveExceptionUseCase(peerType, exception)
            _uiState.update { it.copy(infoMessage = "Exception updated for dialog ${exception.dialogId}") }
        }
    }

    private fun removeException(peerType: GallerySavePeerType, dialogId: Long) {
        viewModelScope.launch {
            removeGallerySaveExceptionUseCase(peerType, dialogId)
            _uiState.update { it.copy(infoMessage = "Exception removed for dialog $dialogId") }
        }
    }

    private fun removeAllExceptions(peerType: GallerySavePeerType) {
        viewModelScope.launch {
            removeAllGallerySaveExceptionsUseCase(peerType)
            _uiState.update { it.copy(infoMessage = "All exceptions removed for ${peerType.name}") }
        }
    }

    private fun dismissMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}
