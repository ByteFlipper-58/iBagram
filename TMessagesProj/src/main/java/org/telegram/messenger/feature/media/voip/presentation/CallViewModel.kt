package org.telegram.messenger.feature.media.voip.presentation

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
import org.telegram.messenger.feature.media.voip.domain.model.CallState
import org.telegram.messenger.feature.media.voip.domain.usecase.AcceptCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.DeclineCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.GetCurrentCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.HangUpCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ObserveCurrentCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.StartCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ToggleMuteUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ToggleSpeakerphoneUseCase

/**
 * ViewModel managing VoIP calling state, actions and UI events.
 */
class CallViewModel(
    private val observeCurrentCallUseCase: ObserveCurrentCallUseCase,
    private val getCurrentCallUseCase: GetCurrentCallUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val acceptCallUseCase: AcceptCallUseCase,
    private val declineCallUseCase: DeclineCallUseCase,
    private val hangUpCallUseCase: HangUpCallUseCase,
    private val toggleMuteUseCase: ToggleMuteUseCase,
    private val toggleSpeakerphoneUseCase: ToggleSpeakerphoneUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _events = Channel<CallEvent>(Channel.BUFFERED)
    val events: Flow<CallEvent> = _events.receiveAsFlow()

    init {
        observeCall()
    }

    private fun observeCall() {
        viewModelScope.launch {
            observeCurrentCallUseCase().collect { call ->
                if (call == null) {
                    _uiState.value = CallUiState.Idle
                } else if (call.state == CallState.ENDED || call.state == CallState.FAILED) {
                    _uiState.value = CallUiState.Ended(reason = call.state.name)
                } else {
                    _uiState.value = CallUiState.Active(call)
                }
            }
        }
    }

    fun startCall(userId: Long, isVideo: Boolean) {
        viewModelScope.launch {
            when (val result = startCallUseCase(userId, isVideo)) {
                is Result.Success -> Unit
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            when (val result = acceptCallUseCase()) {
                is Result.Success -> _events.send(CallEvent.CallAccepted)
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            when (val result = declineCallUseCase()) {
                is Result.Success -> _events.send(CallEvent.CallDeclined)
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }

    fun hangUp() {
        viewModelScope.launch {
            when (val result = hangUpCallUseCase()) {
                is Result.Success -> _events.send(CallEvent.CallEnded)
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            when (val result = toggleMuteUseCase()) {
                is Result.Success -> _events.send(CallEvent.MuteToggled(result.data))
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }

    fun toggleSpeakerphone() {
        viewModelScope.launch {
            when (val result = toggleSpeakerphoneUseCase()) {
                is Result.Success -> _events.send(CallEvent.SpeakerphoneToggled(result.data))
                is Result.Failure -> _events.send(CallEvent.ShowError(result.error.message))
            }
        }
    }
}
