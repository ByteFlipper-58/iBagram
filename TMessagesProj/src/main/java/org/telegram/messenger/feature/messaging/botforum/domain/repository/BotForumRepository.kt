package org.telegram.messenger.feature.messaging.botforum.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState

/**
 * Pure domain repository contract for Bot Forum topics and AI draft streaming.
 */
interface BotForumRepository {

    /**
     * Observes the reactive state of the Bot Forum feature.
     */
    fun observeState(): StateFlow<BotForumState>

    /**
     * Returns an immediate snapshot of the Bot Forum state.
     */
    fun getState(): BotForumState

    /**
     * Resolves the streaming send button state for a given user and topic.
     */
    fun getStreamingSendButtonState(userId: Long, topicId: Int): StreamingSendButtonState

    /**
     * Checks if a topic is currently marked as streaming.
     */
    fun isStreamingTopic(dialogId: Long, topicId: Long): Boolean

    /**
     * Persists or updates the streaming flag for a specific topic.
     */
    fun saveIsStreamingTopic(dialogId: Long, topicId: Long, isStreaming: Boolean)

    /**
     * Checks whether there are active (non-removed) bot forum drafts for the given dialog topic.
     */
    fun hasBotForumDrafts(userId: Long, topicId: Int): Boolean

    /**
     * Stops streaming in the specified bot forum topic, blocklisting the randomId and sending stop draft action.
     */
    fun stopStreaming(userId: Long, topicId: Long)

    /**
     * Cleans up all drafts marked as removed for the topic.
     */
    fun removeAllMarkedAsRemovedMessages(userId: Long, topicId: Int)

    /**
     * Checks if an incoming message replaces an existing streaming draft and returns the replaced draft.
     */
    fun checkNewMessageDraftReplacement(userId: Long, topicId: Int, messageText: String?): BotDraftMessageModel?

    /**
     * Called when a new or updated draft arrives from MTProto.
     */
    fun onBotDraftUpdate(
        userId: Long,
        topicId: Int,
        randomId: Long,
        text: String,
        canStop: Boolean,
        keepOnStop: Boolean,
        isRich: Boolean = false
    )

    /**
     * Called when a draft times out according to TTL.
     */
    fun onBotDraftTimeout(userId: Long, topicId: Int, randomId: Long)

    /**
     * Checks whether a dialog represents a Bot Forum.
     */
    fun isBotForum(dialogId: Long): Boolean

    /**
     * Clears all cached drafts and states.
     */
    fun clearAll()
}
