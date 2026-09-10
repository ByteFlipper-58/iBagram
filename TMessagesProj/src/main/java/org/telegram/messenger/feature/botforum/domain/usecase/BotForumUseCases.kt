package org.telegram.messenger.feature.botforum.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.botforum.domain.model.StreamingSendButtonState
import org.telegram.messenger.feature.botforum.domain.repository.BotForumRepository

/**
 * Derives a clean forum topic title from the initial message text.
 * Truncates to 16 characters with ellipsis or falls back to a default title.
 */
class DeriveTopicNameFromMessageUseCase {
    operator fun invoke(messageText: String?, fallbackTitle: String = "#New Chat"): String {
        val trimmed = messageText?.trim()
        return when {
            trimmed.isNullOrEmpty() -> fallbackTitle
            trimmed.length > 16 -> trimmed.substring(0, 16) + "..."
            else -> trimmed
        }
    }
}

/**
 * Resolves the streaming send button state from a collection of active drafts for a topic.
 */
class ResolveStreamingButtonStateUseCase {
    operator fun invoke(drafts: Collection<BotDraftMessageModel>?): StreamingSendButtonState {
        if (drafts.isNullOrEmpty()) {
            return StreamingSendButtonState.NO_STREAMING
        }
        val activeDraft = drafts.firstOrNull { !it.isRemoved }
            ?: return StreamingSendButtonState.NO_STREAMING

        return if (activeDraft.canStop) {
            StreamingSendButtonState.STOP
        } else {
            StreamingSendButtonState.BLOCKING
        }
    }
}

class ObserveBotForumStateUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(): StateFlow<BotForumState> = repository.observeState()
}

class GetBotForumStateUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(): BotForumState = repository.getState()
}

class GetStreamingSendButtonStateUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(userId: Long, topicId: Int): StreamingSendButtonState =
        repository.getStreamingSendButtonState(userId, topicId)
}

class CheckIsStreamingTopicUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(dialogId: Long, topicId: Long): Boolean =
        repository.isStreamingTopic(dialogId, topicId)
}

class SaveIsStreamingTopicUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(dialogId: Long, topicId: Long, isStreaming: Boolean) {
        repository.saveIsStreamingTopic(dialogId, topicId, isStreaming)
    }
}

class CheckHasBotForumDraftsUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(userId: Long, topicId: Int): Boolean =
        repository.hasBotForumDrafts(userId, topicId)
}

class StopStreamingDraftUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(userId: Long, topicId: Long) {
        repository.stopStreaming(userId, topicId)
    }
}

class UpdateBotForumDraftUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(
        userId: Long,
        topicId: Int,
        randomId: Long,
        text: String,
        canStop: Boolean,
        keepOnStop: Boolean,
        isRich: Boolean = false
    ) {
        repository.onBotDraftUpdate(userId, topicId, randomId, text, canStop, keepOnStop, isRich)
    }
}

class RemoveMarkedRemovedDraftsUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(userId: Long, topicId: Int) {
        repository.removeAllMarkedAsRemovedMessages(userId, topicId)
    }
}

class CheckNewMessageDraftReplacementUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(userId: Long, topicId: Int, messageText: String?): BotDraftMessageModel? =
        repository.checkNewMessageDraftReplacement(userId, topicId, messageText)
}

class CheckIsBotForumUseCase(
    private val repository: BotForumRepository
) {
    operator fun invoke(dialogId: Long): Boolean = repository.isBotForum(dialogId)
}
