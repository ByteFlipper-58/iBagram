package org.telegram.messenger.feature.pushlistener.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.pushlistener.domain.usecase.ObservePushListenerStateUseCase
import org.telegram.messenger.feature.pushlistener.domain.usecase.ProcessIncomingPushUseCase
import org.telegram.messenger.feature.pushlistener.domain.usecase.RegisterPushListenerTokenUseCase
import org.telegram.messenger.feature.pushlistener.domain.usecase.TogglePushListeningUseCase

class PushListenerViewModel(
    private val observeStateUseCase: ObservePushListenerStateUseCase,
    private val processPushUseCase: ProcessIncomingPushUseCase,
    private val registerTokenUseCase: RegisterPushListenerTokenUseCase,
    private val toggleListeningUseCase: TogglePushListeningUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(PushListenerUiState())
    val uiState: StateFlow<PushListenerUiState> = _uiState.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        observeStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        isListening = state.isListening,
                        registeredTokens = state.registeredTokens,
                        lastPush = state.lastProcessedPush?.let { formatPush(it) },
                        totalReceived = state.totalReceivedPushes,
                        totalErrors = state.totalDecryptErrors,
                        error = state.lastError
                    )
                }
            }
            .launchIn(scope)
    }

    fun onEvent(event: PushListenerEvent) {
        when (event) {
            is PushListenerEvent.ProcessPush -> {
                scope.launch {
                    val result = processPushUseCase(event.pushType, event.rawData)
                    if (result.isHandled) {
                        _uiState.update {
                            it.copy(infoMessage = "Push processed: ${result.payload?.actionType}")
                        }
                    } else {
                        _uiState.update {
                            it.copy(error = result.errorMessage ?: "Failed to handle push")
                        }
                    }
                }
            }
            is PushListenerEvent.RegisterToken -> {
                scope.launch {
                    registerTokenUseCase(event.pushType, event.token)
                    _uiState.update {
                        it.copy(infoMessage = "Token registered for ${event.pushType.name}")
                    }
                }
            }
            is PushListenerEvent.ToggleListening -> {
                scope.launch {
                    toggleListeningUseCase(event.enabled)
                }
            }
            PushListenerEvent.ClearHistory -> {
                _uiState.update {
                    it.copy(lastPush = null, totalReceived = 0, totalErrors = 0, infoMessage = "History cleared")
                }
            }
            PushListenerEvent.ClearMessage -> {
                _uiState.update { it.copy(infoMessage = null, error = null) }
            }
        }
    }

    private fun formatPush(payload: PushPayloadModel): FormattedPushItem {
        return FormattedPushItem(
            pushType = payload.pushType.name,
            actionType = payload.actionType.name,
            locKey = payload.locKey,
            dialogId = payload.dialogId,
            topicId = payload.topicId,
            isSilent = payload.isSilent,
            timestamp = payload.receiveTimeMs
        )
    }

    fun onCleared() {
        scope.cancel()
    }
}
