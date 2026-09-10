package org.telegram.messenger.feature.messaging.botforum.presentation

/**
 * MVI Events for Bot Forum topics and draft streaming.
 */
sealed class BotForumEvent {
    data class SelectTopic(val dialogId: Long, val topicId: Int) : BotForumEvent()
    data class StopStreaming(val userId: Long, val topicId: Long) : BotForumEvent()
    data class SetStreamingTopic(val dialogId: Long, val topicId: Long, val isStreaming: Boolean) : BotForumEvent()
    data class OnDraftUpdate(
        val userId: Long,
        val topicId: Int,
        val randomId: Long,
        val text: String,
        val canStop: Boolean,
        val keepOnStop: Boolean,
        val isRich: Boolean = false
    ) : BotForumEvent()
    data class OnDraftTimeout(val userId: Long, val topicId: Int, val randomId: Long) : BotForumEvent()
    data class CheckDraftReplacement(val userId: Long, val topicId: Int, val messageText: String?) : BotForumEvent()
    data class CleanRemovedDrafts(val userId: Long, val topicId: Int) : BotForumEvent()
    object ClearAll : BotForumEvent()
    object DismissError : BotForumEvent()
}
