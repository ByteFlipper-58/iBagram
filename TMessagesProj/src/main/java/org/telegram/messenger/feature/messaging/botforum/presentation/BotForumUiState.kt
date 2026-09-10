package org.telegram.messenger.feature.messaging.botforum.presentation

import org.telegram.messenger.feature.messaging.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState

/**
 * UI State for the Bot Forum feature.
 */
data class BotForumUiState(
    val state: BotForumState = BotForumState(),
    val selectedDialogId: Long? = null,
    val selectedTopicId: Int? = null,
    val streamingButtonState: StreamingSendButtonState = StreamingSendButtonState.NO_STREAMING,
    val isStreamingActive: Boolean = false,
    val activeDraftsForCurrentTopic: List<BotDraftMessageModel> = emptyList(),
    val lastReplacedDraft: BotDraftMessageModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
