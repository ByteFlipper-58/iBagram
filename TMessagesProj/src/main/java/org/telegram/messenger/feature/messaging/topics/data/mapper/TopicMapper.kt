package org.telegram.messenger.feature.messaging.topics.data.mapper

import org.telegram.messenger.feature.messaging.topics.domain.model.TopicModel
import org.telegram.tgnet.TLRPC

/**
 * Pure mapper between [TLRPC.TL_forumTopic] and [TopicModel].
 */
object TopicMapper {

    fun toDomain(chatId: Long, topic: TLRPC.TL_forumTopic?): TopicModel? {
        if (topic == null) return null
        return TopicModel(
            id = topic.id.toLong(),
            chatId = chatId,
            title = topic.title ?: "",
            iconColor = topic.icon_color,
            iconEmojiId = topic.icon_emoji_id,
            isClosed = topic.closed,
            isPinned = topic.pinned,
            isHidden = topic.hidden,
            isShort = topic.isShort,
            date = topic.date.toLong(),
            topMessageId = topic.top_message.toLong(),
            readInboxMaxId = topic.read_inbox_max_id.toLong(),
            readOutboxMaxId = topic.read_outbox_max_id.toLong(),
            unreadCount = topic.unread_count,
            unreadMentionsCount = topic.unread_mentions_count,
            unreadReactionsCount = topic.unread_reactions_count,
            unreadPollVotesCount = topic.unread_poll_votes_count,
            pinnedOrder = topic.pinnedOrder,
            totalMessagesCount = topic.totalMessagesCount
        )
    }

    fun toDomainList(chatId: Long, topics: List<TLRPC.TL_forumTopic>?): List<TopicModel> {
        if (topics.isNullOrEmpty()) return emptyList()
        return topics.mapNotNull { toDomain(chatId, it) }
    }
}
