package org.telegram.messenger.feature.messaging.chatmeta.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CancelPendingMetadataRequestsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CheckMessagesMetadataUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.GetChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesExtendedMediaUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesReactionsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.ObserveChatMetadataStatsUseCase

class ChatMetadataViewModel(
    private val observeChatMetadataStatsUseCase: ObserveChatMetadataStatsUseCase,
    private val getChatMetadataStatsUseCase: GetChatMetadataStatsUseCase,
    private val checkMessagesMetadataUseCase: CheckMessagesMetadataUseCase,
    private val loadMessagesReactionsUseCase: LoadMessagesReactionsUseCase,
    private val loadMessagesExtendedMediaUseCase: LoadMessagesExtendedMediaUseCase,
    private val cancelPendingMetadataRequestsUseCase: CancelPendingMetadataRequestsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatMetadataUiState(stats = getChatMetadataStatsUseCase()))
    val uiState: StateFlow<ChatMetadataUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeChatMetadataStatsUseCase().collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats)
            }
        }
    }

    fun onEvent(event: ChatMetadataEvent) {
        when (event) {
            is ChatMetadataEvent.CheckMessages -> checkMessages(event.dialogId, event.items, event.currentTime)
            is ChatMetadataEvent.LoadReactions -> loadReactions(event.dialogId, event.messageIds)
            is ChatMetadataEvent.LoadExtendedMedia -> loadExtendedMedia(event.dialogId, event.messageIds)
            is ChatMetadataEvent.CancelPending -> cancelPending()
            is ChatMetadataEvent.DismissInfo -> dismissInfo()
        }
    }

    private fun checkMessages(dialogId: Long, items: List<MessageMetadataCheckItem>, currentTime: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true)
            when (val result = checkMessagesMetadataUseCase(dialogId, items, currentTime)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lastBatchResult = result.data,
                        isChecking = false
                    )
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        infoMessage = "Failed to check messages metadata"
                    )
                }
            }
        }
    }

    private fun loadReactions(dialogId: Long, messageIds: List<Int>) {
        loadMessagesReactionsUseCase(dialogId, messageIds)
    }

    private fun loadExtendedMedia(dialogId: Long, messageIds: List<Int>) {
        loadMessagesExtendedMediaUseCase(dialogId, messageIds)
    }

    private fun cancelPending() {
        cancelPendingMetadataRequestsUseCase()
    }

    private fun dismissInfo() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        cancelPending()
    }
}
