package org.telegram.messenger.feature.chatinput.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.chatinput.domain.repository.ChatInputRepository
import org.telegram.messenger.feature.chatinput.domain.usecase.FormatTextSelectionUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ResolvePanelVisibilityUseCase

/**
 * ViewModel orchestrating ChatActivityEnterView MVI events and StateFlow.
 */
class ChatInputViewModel(
    private val repository: ChatInputRepository,
    private val formatUseCase: FormatTextSelectionUseCase = FormatTextSelectionUseCase(),
    private val resolvePanelUseCase: ResolvePanelVisibilityUseCase = ResolvePanelVisibilityUseCase(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
) {

    private val _uiState = MutableStateFlow(ChatInputUiState.fromDomain(repository.getState()))
    val uiState: StateFlow<ChatInputUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeState().collect { domainState ->
                _uiState.value = ChatInputUiState.fromDomain(domainState)
            }
        }
    }

    fun onEvent(event: ChatInputEvent) {
        when (event) {
            is ChatInputEvent.TextChanged -> {
                repository.setText(event.text, event.selectionStart, event.selectionEnd)
            }
            is ChatInputEvent.ApplyTextFormat -> {
                val current = repository.getState()
                val formatted = formatUseCase(
                    originalText = current.text,
                    selectionStart = current.selectionStart,
                    selectionEnd = current.selectionEnd,
                    style = event.style
                )
                repository.setText(
                    text = formatted.newText,
                    selectionStart = formatted.newSelectionStart,
                    selectionEnd = formatted.newSelectionEnd
                )
            }
            is ChatInputEvent.TogglePanel -> {
                val current = repository.getState()
                val resolved = resolvePanelUseCase(current.panelMode, event.mode)
                repository.setPanelMode(resolved)
            }
            is ChatInputEvent.SetReply -> {
                repository.setReplyMessage(event.messageId, event.quote, event.quoteOffset)
            }
            is ChatInputEvent.SetEdit -> {
                repository.setEditMessage(event.messageId, event.text)
            }
            is ChatInputEvent.ClearReplyOrEdit -> {
                repository.clearReplyOrEdit()
            }
            is ChatInputEvent.UpdateSendOptions -> {
                repository.updateSendOptions(event.options)
            }
            is ChatInputEvent.StartRecording -> {
                repository.startRecording(event.type)
            }
            is ChatInputEvent.LockRecording -> {
                repository.lockRecording()
            }
            is ChatInputEvent.PauseRecording -> {
                repository.pauseRecording()
            }
            is ChatInputEvent.ResumeRecording -> {
                repository.resumeRecording()
            }
            is ChatInputEvent.CancelRecording -> {
                repository.cancelRecording()
            }
            is ChatInputEvent.StopRecording -> {
                repository.stopRecording()
            }
            is ChatInputEvent.RecordProgressUpdated -> {
                repository.updateRecordProgress(event.durationMs, event.amplitude)
            }
            is ChatInputEvent.ToggleVoiceOnce -> {
                repository.toggleVoiceOnce()
            }
            is ChatInputEvent.Reset -> {
                repository.reset()
            }
        }
        _uiState.value = ChatInputUiState.fromDomain(repository.getState())
    }

    fun clear() {
        scope.cancel()
    }
}
