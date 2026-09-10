package org.telegram.messenger.feature.botforum.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.botforum.domain.model.StreamingSendButtonState
import org.telegram.messenger.feature.botforum.domain.usecase.CheckHasBotForumDraftsUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.CheckIsStreamingTopicUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.CheckNewMessageDraftReplacementUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.GetStreamingSendButtonStateUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.ObserveBotForumStateUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.RemoveMarkedRemovedDraftsUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.SaveIsStreamingTopicUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.StopStreamingDraftUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.UpdateBotForumDraftUseCase

/**
 * MVI ViewModel for managing bot forum topic draft streaming and send button states.
 */
class BotForumViewModel(
    private val observeBotForumStateUseCase: ObserveBotForumStateUseCase,
    private val getStreamingSendButtonStateUseCase: GetStreamingSendButtonStateUseCase,
    private val checkIsStreamingTopicUseCase: CheckIsStreamingTopicUseCase,
    private val saveIsStreamingTopicUseCase: SaveIsStreamingTopicUseCase,
    private val stopStreamingDraftUseCase: StopStreamingDraftUseCase,
    private val updateBotForumDraftUseCase: UpdateBotForumDraftUseCase,
    private val removeMarkedRemovedDraftsUseCase: RemoveMarkedRemovedDraftsUseCase,
    private val checkNewMessageDraftReplacementUseCase: CheckNewMessageDraftReplacementUseCase,
    private val checkHasBotForumDraftsUseCase: CheckHasBotForumDraftsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotForumUiState())
    val uiState: StateFlow<BotForumUiState> = _uiState.asStateFlow()

    init {
        observeBotForumStateUseCase()
            .onEach { domainState ->
                _uiState.update { curr ->
                    val dialogId = curr.selectedDialogId
                    val topicId = curr.selectedTopicId
                    val (buttonState, isStreamingActive, activeDrafts) = if (dialogId != null && topicId != null) {
                        val bState = getStreamingSendButtonStateUseCase(dialogId, topicId)
                        val isStreaming = checkIsStreamingTopicUseCase(dialogId, topicId.toLong())
                        val drafts = domainState.activeDrafts.filterKeys { it.first == dialogId && it.second == topicId }.values.toList()
                        Triple(bState, isStreaming, drafts)
                    } else {
                        Triple(StreamingSendButtonState.NO_STREAMING, false, emptyList<BotDraftMessageModel>())
                    }

                    curr.copy(
                        state = domainState,
                        streamingButtonState = buttonState,
                        isStreamingActive = isStreamingActive,
                        activeDraftsForCurrentTopic = activeDrafts,
                        errorMessage = domainState.errorMessage
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BotForumEvent) {
        when (event) {
            is BotForumEvent.SelectTopic -> {
                val buttonState = getStreamingSendButtonStateUseCase(event.dialogId, event.topicId)
                val isStreaming = checkIsStreamingTopicUseCase(event.dialogId, event.topicId.toLong())
                val drafts = _uiState.value.state.activeDrafts.filterKeys {
                    it.first == event.dialogId && it.second == event.topicId
                }.values.toList()

                _uiState.update {
                    it.copy(
                        selectedDialogId = event.dialogId,
                        selectedTopicId = event.topicId,
                        streamingButtonState = buttonState,
                        isStreamingActive = isStreaming,
                        activeDraftsForCurrentTopic = drafts,
                        errorMessage = null
                    )
                }
            }
            is BotForumEvent.StopStreaming -> {
                stopStreamingDraftUseCase(event.userId, event.topicId)
                saveIsStreamingTopicUseCase(event.userId, event.topicId, false)
                _uiState.update {
                    it.copy(
                        isStreamingActive = false,
                        streamingButtonState = getStreamingSendButtonStateUseCase(event.userId, event.topicId.toInt())
                    )
                }
            }
            is BotForumEvent.SetStreamingTopic -> {
                saveIsStreamingTopicUseCase(event.dialogId, event.topicId, event.isStreaming)
                if (_uiState.value.selectedDialogId == event.dialogId && _uiState.value.selectedTopicId == event.topicId.toInt()) {
                    _uiState.update { it.copy(isStreamingActive = event.isStreaming) }
                }
            }
            is BotForumEvent.OnDraftUpdate -> {
                updateBotForumDraftUseCase(
                    userId = event.userId,
                    topicId = event.topicId,
                    randomId = event.randomId,
                    text = event.text,
                    canStop = event.canStop,
                    keepOnStop = event.keepOnStop,
                    isRich = event.isRich
                )
            }
            is BotForumEvent.OnDraftTimeout -> {
                // Handled in repository via onBotDraftTimeout
            }
            is BotForumEvent.CheckDraftReplacement -> {
                val replaced = checkNewMessageDraftReplacementUseCase(event.userId, event.topicId, event.messageText)
                _uiState.update { it.copy(lastReplacedDraft = replaced) }
            }
            is BotForumEvent.CleanRemovedDrafts -> {
                removeMarkedRemovedDraftsUseCase(event.userId, event.topicId)
            }
            is BotForumEvent.ClearAll -> {
                _uiState.update { BotForumUiState() }
            }
            is BotForumEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
