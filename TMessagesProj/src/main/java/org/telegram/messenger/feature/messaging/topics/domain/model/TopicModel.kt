package org.telegram.messenger.feature.messaging.topics.domain.model

/**
 * Pure Kotlin domain model representing a forum topic in a supergroup.
 */
data class TopicModel(
    val id: Long,
    val chatId: Long,
    val title: String,
    val iconColor: Int = 0,
    val iconEmojiId: Long = 0L,
    val isClosed: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isShort: Boolean = false,
    val date: Long = 0L,
    val topMessageId: Long = 0L,
    val readInboxMaxId: Long = 0L,
    val readOutboxMaxId: Long = 0L,
    val unreadCount: Int = 0,
    val unreadMentionsCount: Int = 0,
    val unreadReactionsCount: Int = 0,
    val unreadPollVotesCount: Int = 0,
    val pinnedOrder: Int = 0,
    val totalMessagesCount: Int = 0
) {
    /**
     * In Telegram forums, topic id == 1 represents the "General" topic.
     */
    val isGeneral: Boolean
        get() = id == 1L
}
