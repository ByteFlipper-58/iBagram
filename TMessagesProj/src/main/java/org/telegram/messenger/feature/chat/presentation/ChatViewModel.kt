package org.telegram.messenger.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chat.domain.usecase.DeleteMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.LoadHistoryUseCase
import org.telegram.messenger.feature.chat.domain.usecase.ObserveMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.SendMessageUseCase

/**
 * ViewModel managing reactive message state, history pagination, and message sending.
 */
class ChatViewModel(
    val account: Int,
    val dialogId: Long,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val loadHistoryUseCase: LoadHistoryUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val deleteMessagesUseCase: DeleteMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        _uiState.value = ChatUiState.Loading

        observeJob = observeMessagesUseCase(dialogId)
            .onEach { list ->
                val current = _uiState.value
                val isSending = (current as? ChatUiState.Success)?.isSending ?: false
                _uiState.value = ChatUiState.Success(
                    messages = list,
                    dialogId = dialogId,
                    isSending = isSending,
                    isLoadingHistory = false
                )
            }
            .catch { error ->
                _uiState.value = ChatUiState.Error(error.message)
            }
            .launchIn(viewModelScope)
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val current = _uiState.value
        if (current is ChatUiState.Success) {
            _uiState.value = current.copy(isSending = true)
        }

        viewModelScope.launch {
            val result = sendMessageUseCase(dialogId, trimmed)
            when (result) {
                is Result.Success -> {
                    _events.emit(ChatEvent.MessageSent)
                    _events.emit(ChatEvent.ScrollToBottom)
                }
                is Result.Failure -> {
                    _events.emit(ChatEvent.ShowError(result.error.message))
                }
            }
            val latest = _uiState.value
            if (latest is ChatUiState.Success) {
                _uiState.value = latest.copy(isSending = false)
            }
        }
    }

    fun onLoadHistory(count: Int = 30) {
        val current = _uiState.value
        if (current is ChatUiState.Success && !current.isLoadingHistory) {
            _uiState.value = current.copy(isLoadingHistory = true)
            viewModelScope.launch {
                val result = loadHistoryUseCase(dialogId, count)
                if (result is Result.Failure) {
                    _events.emit(ChatEvent.ShowError(result.error.message))
                }
                val latest = _uiState.value
                if (latest is ChatUiState.Success) {
                    _uiState.value = latest.copy(isLoadingHistory = false)
                }
            }
        }
    }

    fun onDeleteMessages(messageIds: List<Int>, revoke: Boolean = true) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            val result = deleteMessagesUseCase(dialogId, messageIds, revoke)
            if (result is Result.Failure) {
                _events.emit(ChatEvent.ShowError(result.error.message))
            }
        }
    }
}
