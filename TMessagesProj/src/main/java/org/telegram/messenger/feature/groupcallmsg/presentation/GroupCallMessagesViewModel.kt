package org.telegram.messenger.feature.groupcallmsg.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ClearGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.GetGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ObserveGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.PopGroupCallMessageUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.SendGroupCallMessageUseCase

/**
 * ViewModel managing ephemeral group call and conference chat messages.
 */
class GroupCallMessagesViewModel(
    private val observeGroupCallMessagesUseCase: ObserveGroupCallMessagesUseCase,
    private val getGroupCallMessagesUseCase: GetGroupCallMessagesUseCase,
    private val sendGroupCallMessageUseCase: SendGroupCallMessageUseCase,
    private val popGroupCallMessageUseCase: PopGroupCallMessageUseCase,
    private val clearGroupCallMessagesUseCase: ClearGroupCallMessagesUseCase,
    initialCallId: Long = 0L
) : ViewModel() {

    private var currentCallId: Long = initialCallId
    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow(
        GroupCallMessagesUiState(state = getGroupCallMessagesUseCase(initialCallId))
    )
    val uiState: StateFlow<GroupCallMessagesUiState> = _uiState.asStateFlow()

    init {
        startObserving(initialCallId)
    }

    fun onEvent(event: GroupCallMessagesEvent) {
        when (event) {
            is GroupCallMessagesEvent.SetCallId -> setCallId(event.callId)
            is GroupCallMessagesEvent.SendMessage -> sendMessage(event.text, event.sendAsPeerId)
            is GroupCallMessagesEvent.PopMessage -> popMessage()
            is GroupCallMessagesEvent.ClearMessages -> clearMessages()
            is GroupCallMessagesEvent.DismissError -> dismissError()
        }
    }

    private fun setCallId(callId: Long) {
        if (currentCallId == callId) return
        currentCallId = callId
        startObserving(callId)
    }

    private fun startObserving(callId: Long) {
        observeJob?.cancel()
        _uiState.update { it.copy(state = getGroupCallMessagesUseCase(callId)) }

        observeJob = observeGroupCallMessagesUseCase(callId)
            .onEach { state ->
                _uiState.update { it.copy(state = state) }
            }
            .launchIn(viewModelScope)
    }

    private fun sendMessage(text: String, sendAsPeerId: Long) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            val success = sendGroupCallMessageUseCase(currentCallId, sendAsPeerId, text.trim())
            if (!success) {
                _uiState.update { it.copy(isSending = false, errorMessage = "Failed to send in-call message") }
            } else {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    private fun popMessage() {
        popGroupCallMessageUseCase(currentCallId)
    }

    private fun clearMessages() {
        clearGroupCallMessagesUseCase(currentCallId)
        _uiState.update { it.copy(state = GroupCallMessagesStateModel(callId = currentCallId)) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
