package org.telegram.messenger.feature.topics.domain.model

/**
 * Domain model representing forum unread counters.
 */
data class ForumUnreadCountModel(
    val chatId: Long,
    val unreadTopicsCount: Int = 0,
    val unreadMessagesCount: Int = 0
)
