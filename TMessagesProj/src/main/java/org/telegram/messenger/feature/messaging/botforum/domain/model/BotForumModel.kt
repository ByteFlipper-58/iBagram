package org.telegram.messenger.feature.messaging.botforum.domain.model

/**
 * State of the chat send button when an AI bot is streaming drafts into a forum topic.
 */
enum class StreamingSendButtonState {
    NO_STREAMING,
    BLOCKING,
    STOP
}

/**
 * Pure domain representation of an in-flight bot streaming draft message.
 */
data class BotDraftMessageModel(
    val userId: Long,
    val topicId: Int,
    val randomId: Long,
    val localMessageId: Int,
    val text: String,
    val canStop: Boolean,
    val keepOnStop: Boolean,
    val isRemoved: Boolean = false,
    val isRich: Boolean = false
)

/**
 * Pure domain representation of a created or existing bot forum topic.
 */
data class BotForumTopicModel(
    val dialogId: Long,
    val topicId: Int,
    val title: String,
    val isMy: Boolean = true
)

/**
 * Domain notification models corresponding to bot forum draft lifecycle events.
 */
data class BotForumDraftUpdateNotificationModel(
    val botUserId: Long,
    val botTopicId: Long,
    val localMessageId: Int,
    val text: String,
    val isNew: Boolean
)

data class BotForumDraftDeleteNotificationModel(
    val botUserId: Long,
    val botTopicId: Long,
    val messageId: Int
)

data class BotForumTopicCreateNotificationModel(
    val dialogId: Long,
    val topicId: Int,
    val title: String
)

/**
 * Aggregated state of the Bot Forum feature.
 */
data class BotForumState(
    val activeDrafts: Map<Triple<Long, Int, Long>, BotDraftMessageModel> = emptyMap(),
    val streamingTopics: Set<Pair<Long, Long>> = emptySet(),
    val blockedRandomIds: Set<Triple<Long, Int, Long>> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
