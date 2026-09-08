package org.telegram.messenger.feature.secretchat.presentation

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
import org.telegram.messenger.feature.secretchat.domain.usecase.AcceptSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.DeclineSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.GetSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.ObserveSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.SendScreenshotNotificationUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.SetSecretChatTtlUseCase

/**
 * ViewModel managing state, lifecycle, and user interactions for a Secret Chat.
 */
class SecretChatViewModel(
    val chatId: Int,
    private val observeSecretChatUseCase: ObserveSecretChatUseCase,
    private val getSecretChatUseCase: GetSecretChatUseCase,
    private val acceptSecretChatUseCase: AcceptSecretChatUseCase,
    private val declineSecretChatUseCase: DeclineSecretChatUseCase,
    private val setSecretChatTtlUseCase: SetSecretChatTtlUseCase,
    private val sendScreenshotNotificationUseCase: SendScreenshotNotificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecretChatUiState>(SecretChatUiState.Loading)
    val uiState: StateFlow<SecretChatUiState> = _uiState.asStateFlow()

    private val _events = Channel<SecretChatEvent>(Channel.BUFFERED)
    val events: Flow<SecretChatEvent> = _events.receiveAsFlow()

    init {
        observeChat()
    }

    private fun observeChat() {
        viewModelScope.launch {
            observeSecretChatUseCase(chatId).collect { chat ->
                if (chat != null) {
                    _uiState.value = SecretChatUiState.Success(chat)
                } else {
                    _uiState.value = SecretChatUiState.Error("Secret chat $chatId not found")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val chat = getSecretChatUseCase(chatId)
            if (chat != null) {
                _uiState.value = SecretChatUiState.Success(chat)
            } else {
                _uiState.value = SecretChatUiState.Error("Secret chat $chatId not found")
            }
        }
    }

    fun accept() {
        viewModelScope.launch {
            when (val result = acceptSecretChatUseCase(chatId)) {
                is Result.Success -> _events.send(SecretChatEvent.ChatAccepted)
                is Result.Failure -> _events.send(SecretChatEvent.ShowError(result.error.message))
            }
        }
    }

    fun decline() {
        viewModelScope.launch {
            when (val result = declineSecretChatUseCase(chatId)) {
                is Result.Success -> _events.send(SecretChatEvent.ChatDeclined)
                is Result.Failure -> _events.send(SecretChatEvent.ShowError(result.error.message))
            }
        }
    }

    fun setTtl(ttlSeconds: Int) {
        viewModelScope.launch {
            when (val result = setSecretChatTtlUseCase(chatId, ttlSeconds)) {
                is Result.Success -> _events.send(SecretChatEvent.TtlUpdated(ttlSeconds))
                is Result.Failure -> _events.send(SecretChatEvent.ShowError(result.error.message))
            }
        }
    }

    fun sendScreenshotNotification() {
        viewModelScope.launch {
            when (val result = sendScreenshotNotificationUseCase(chatId)) {
                is Result.Success -> _events.send(SecretChatEvent.ScreenshotSent)
                is Result.Failure -> _events.send(SecretChatEvent.ShowError(result.error.message))
            }
        }
    }
}
