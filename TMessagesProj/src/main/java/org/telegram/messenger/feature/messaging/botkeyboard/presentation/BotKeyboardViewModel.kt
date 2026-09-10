package org.telegram.messenger.feature.messaging.botkeyboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ClearAllKeyboardsUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.GetKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ObserveBotKeyboardStateUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RecordButtonPressedUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RemoveKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.SetKeyboardForMessageUseCase

class BotKeyboardViewModel(
    private val getKeyboardUseCase: GetKeyboardForMessageUseCase,
    private val setKeyboardUseCase: SetKeyboardForMessageUseCase,
    private val removeKeyboardUseCase: RemoveKeyboardForMessageUseCase,
    private val clearAllKeyboardsUseCase: ClearAllKeyboardsUseCase,
    private val recordButtonPressedUseCase: RecordButtonPressedUseCase,
    private val observeStateUseCase: ObserveBotKeyboardStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotKeyboardUiState())
    val uiState: StateFlow<BotKeyboardUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { state ->
                val currentMsgId = _uiState.value.currentMessageId
                val layout = state.activeKeyboards[currentMsgId]
                _uiState.update { current ->
                    current.copy(
                        currentLayout = layout,
                        lastPressedButton = state.lastPressedButton,
                        totalKeyboardsCount = state.activeKeyboards.size
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BotKeyboardEvent) {
        when (event) {
            is BotKeyboardEvent.SelectMessage -> {
                val layout = getKeyboardUseCase(event.messageId)
                _uiState.update { current ->
                    current.copy(
                        currentMessageId = event.messageId,
                        currentLayout = layout
                    )
                }
            }

            is BotKeyboardEvent.PressButton -> {
                recordButtonPressedUseCase(event.button)
                _uiState.update { current ->
                    current.copy(lastPressedButton = event.button)
                }
            }

            is BotKeyboardEvent.SetLayout -> {
                setKeyboardUseCase(event.messageId, event.layout)
                if (event.messageId == _uiState.value.currentMessageId) {
                    _uiState.update { current ->
                        current.copy(currentLayout = event.layout)
                    }
                }
            }

            is BotKeyboardEvent.RemoveLayout -> {
                removeKeyboardUseCase(event.messageId)
                if (event.messageId == _uiState.value.currentMessageId) {
                    _uiState.update { current ->
                        current.copy(currentLayout = null)
                    }
                }
            }

            is BotKeyboardEvent.ClearAll -> {
                clearAllKeyboardsUseCase()
                _uiState.update { current ->
                    current.copy(
                        currentLayout = null,
                        totalKeyboardsCount = 0
                    )
                }
            }

            is BotKeyboardEvent.DismissError -> {
                _uiState.update { current ->
                    current.copy(errorMessage = null)
                }
            }
        }
    }
}
