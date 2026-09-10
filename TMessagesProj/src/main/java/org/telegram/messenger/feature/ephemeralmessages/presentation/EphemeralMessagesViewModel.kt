package org.telegram.messenger.feature.ephemeralmessages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ClearAllWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.GetWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.IsEphemeralCommandUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ObserveEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ParseBotCommandUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.PutWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.RemoveWelcomeAnchorBindingUseCase

class EphemeralMessagesViewModel(
    private val parseBotCommandUseCase: ParseBotCommandUseCase,
    private val isEphemeralCommandUseCase: IsEphemeralCommandUseCase,
    private val putWelcomeAnchorBindingUseCase: PutWelcomeAnchorBindingUseCase,
    private val removeWelcomeAnchorBindingUseCase: RemoveWelcomeAnchorBindingUseCase,
    private val getWelcomeAnchorBindingsUseCase: GetWelcomeAnchorBindingsUseCase,
    private val clearAllWelcomeAnchorBindingsUseCase: ClearAllWelcomeAnchorBindingsUseCase,
    private val observeStateUseCase: ObserveEphemeralMessagesStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EphemeralMessagesUiState())
    val uiState: StateFlow<EphemeralMessagesUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { state ->
                val dialogId = _uiState.value.currentDialogId
                val bindings = state.activeAnchorBindings[dialogId] ?: emptyMap()
                _uiState.update { current ->
                    current.copy(activeAnchorBindings = bindings)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EphemeralMessagesEvent) {
        when (event) {
            is EphemeralMessagesEvent.InputTextChanged -> {
                val parsed = parseBotCommandUseCase(event.text)
                val isEphemeral = if (parsed != null) {
                    isEphemeralCommandUseCase(event.text, event.dialogId)
                } else {
                    false
                }

                _uiState.update { current ->
                    current.copy(
                        currentDialogId = event.dialogId,
                        isCurrentInputEphemeral = isEphemeral,
                        currentCommandInfo = parsed?.copy(isEphemeral = isEphemeral)
                    )
                }
            }

            is EphemeralMessagesEvent.RegisterAnchor -> {
                putWelcomeAnchorBindingUseCase(
                    dialogId = event.dialogId,
                    messageId = event.messageId,
                    ephemeralMessageId = event.ephemeralMessageId
                )
            }

            is EphemeralMessagesEvent.UnregisterAnchor -> {
                removeWelcomeAnchorBindingUseCase(
                    dialogId = event.dialogId,
                    messageId = event.messageId,
                    ephemeralMessageId = event.ephemeralMessageId
                )
            }

            is EphemeralMessagesEvent.SelectDialog -> {
                val bindings = getWelcomeAnchorBindingsUseCase(event.dialogId)
                _uiState.update { current ->
                    current.copy(
                        currentDialogId = event.dialogId,
                        activeAnchorBindings = bindings,
                        isCurrentInputEphemeral = false,
                        currentCommandInfo = null
                    )
                }
            }

            is EphemeralMessagesEvent.ClearDialogAnchors -> {
                val bindings = getWelcomeAnchorBindingsUseCase(event.dialogId)
                for ((msgId, ephId) in bindings) {
                    removeWelcomeAnchorBindingUseCase(event.dialogId, msgId, ephId)
                }
            }

            is EphemeralMessagesEvent.ClearAllAnchors -> {
                clearAllWelcomeAnchorBindingsUseCase()
            }

            is EphemeralMessagesEvent.DismissError -> {
                _uiState.update { current ->
                    current.copy(errorMessage = null)
                }
            }
        }
    }
}
