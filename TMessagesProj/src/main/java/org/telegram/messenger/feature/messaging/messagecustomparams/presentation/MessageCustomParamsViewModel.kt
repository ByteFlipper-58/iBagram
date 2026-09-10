package org.telegram.messenger.feature.messaging.messagecustomparams.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.repository.MessageCustomParamsRepository
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ClearAllMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CopyMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ObserveMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.RemoveMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.SetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageSummaryUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageTranslationUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateVoiceTranscriptionUseCase

class MessageCustomParamsViewModel(
    private val observeState: ObserveMessageCustomParamsStateUseCase,
    private val getParams: GetMessageCustomParamsUseCase,
    private val setParams: SetMessageCustomParamsUseCase,
    private val updateVoice: UpdateVoiceTranscriptionUseCase,
    private val updateTranslation: UpdateMessageTranslationUseCase,
    private val updateSummary: UpdateMessageSummaryUseCase,
    private val copyParams: CopyMessageCustomParamsUseCase,
    private val removeParams: RemoveMessageCustomParamsUseCase,
    private val clearAll: ClearAllMessageCustomParamsUseCase,
    private val repository: MessageCustomParamsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageCustomParamsUiState())
    val uiState: StateFlow<MessageCustomParamsUiState> = _uiState.asStateFlow()

    init {
        observeState()
            .onEach { state ->
                _uiState.update { it.copy(state = state) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MessageCustomParamsEvent) {
        when (event) {
            is MessageCustomParamsEvent.LoadParams -> {
                val params = getParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = params,
                        isTranscriptionOpen = params?.voiceTranscription?.isOpen ?: false,
                        isSummaryOpen = params?.summary?.isSummarizedOpen ?: false,
                        statusMessage = "Params loaded"
                    )
                }
            }
            is MessageCustomParamsEvent.SetParams -> {
                setParams(event.messageId, event.params)
                val updated = getParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = updated,
                        isTranscriptionOpen = updated?.voiceTranscription?.isOpen ?: false,
                        isSummaryOpen = updated?.summary?.isSummarizedOpen ?: false,
                        statusMessage = "Params updated",
                        errorMessage = null
                    )
                }
            }
            is MessageCustomParamsEvent.UpdateTranscription -> {
                updateVoice(
                    messageId = event.messageId,
                    text = event.text,
                    isFinal = event.isFinal,
                    isOpen = event.isOpen
                )
                val updated = getParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = updated,
                        isTranscriptionOpen = event.isOpen,
                        statusMessage = "Transcription updated",
                        errorMessage = null
                    )
                }
            }
            is MessageCustomParamsEvent.UpdateTranslation -> {
                updateTranslation(
                    messageId = event.messageId,
                    targetLanguage = event.targetLanguage,
                    translatedText = event.translatedText
                )
                val updated = getParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = updated,
                        statusMessage = "Translation updated",
                        errorMessage = null
                    )
                }
            }
            is MessageCustomParamsEvent.UpdateSummary -> {
                updateSummary(
                    messageId = event.messageId,
                    summaryText = event.summaryText,
                    isSummarizedOpen = event.isOpen
                )
                val updated = getParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = updated,
                        isSummaryOpen = event.isOpen,
                        statusMessage = "Summary updated",
                        errorMessage = null
                    )
                }
            }
            is MessageCustomParamsEvent.CopyParams -> {
                copyParams(event.fromMessageId, event.toMessageId)
                val target = getParams(event.toMessageId)
                _uiState.update {
                    it.copy(
                        currentParams = target,
                        statusMessage = "Params copied",
                        errorMessage = null
                    )
                }
            }
            is MessageCustomParamsEvent.RemoveParams -> {
                removeParams(event.messageId)
                _uiState.update {
                    it.copy(
                        currentParams = if (it.currentParams?.messageId == event.messageId) null else it.currentParams,
                        statusMessage = "Params removed",
                        errorMessage = null
                    )
                }
            }
            MessageCustomParamsEvent.ClearAll -> {
                clearAll()
                _uiState.update {
                    it.copy(
                        currentParams = null,
                        statusMessage = "All params cleared",
                        errorMessage = null
                    )
                }
            }
            MessageCustomParamsEvent.DismissMessage -> {
                _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
            }
        }
    }
}
