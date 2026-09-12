package org.telegram.messenger.feature.messaging.topics.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_forum

/**
 * Remote data source executing MTProto RPCs for forum topics.
 */
open class TopicsRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getForumTopics(
        chatId: Long,
        offsetDate: Int = 0,
        offsetId: Int = 0,
        offsetTopic: Int = 0,
        limit: Int = 100
    ): Result<TLRPC.TL_messages_forumTopics> {
        val req = TL_forum.TL_messages_getForumTopics().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.offset_date = offsetDate
            this.offset_id = offsetId
            this.offset_topic = offsetTopic
            this.limit = limit
        }
        return executeRequest<TLRPC.TL_messages_forumTopics>(req)
    }

    open suspend fun getSavedDialogsForForum(
        chatId: Long,
        offsetDate: Int = 0,
        offsetId: Int = 0,
        limit: Int = 100
    ): Result<TLRPC.TL_messages_savedDialogs> {
        val req = TLRPC.TL_messages_getSavedDialogs().apply {
            parent_peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            flags = flags or 2
            this.offset_date = offsetDate
            this.offset_id = offsetId
            this.offset_peer = TLRPC.TL_inputPeerEmpty()
            this.limit = limit
        }
        return executeRequest(req)
    }

    open suspend fun editForumTopic(
        chatId: Long,
        topicId: Int,
        flags: Int,
        closed: Boolean = false,
        hidden: Boolean = false
    ): Result<TLRPC.Updates> {
        val req = TL_forum.TL_messages_editForumTopic().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.topic_id = topicId
            this.flags = flags
            this.closed = closed
            this.hidden = hidden
        }
        return executeRequest(req)
    }

    open suspend fun updatePinnedForumTopic(
        chatId: Long,
        topicId: Int,
        pinned: Boolean
    ): Result<TLRPC.Updates> {
        val req = TL_forum.TL_messages_updatePinnedForumTopic().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.topic_id = topicId
            this.pinned = pinned
        }
        return executeRequest(req)
    }

    open suspend fun deleteTopicHistory(
        chatId: Long,
        topicId: Int
    ): Result<TLRPC.TL_messages_affectedHistory> {
        val req = TL_forum.TL_messages_deleteTopicHistory().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.top_msg_id = topicId
        }
        return executeRequest(req)
    }

    open suspend fun reorderPinnedTopics(
        chatId: Long,
        topicIds: List<Int>
    ): Result<TLRPC.Updates> {
        val req = TL_forum.TL_messages_reorderPinnedForumTopics().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.order.addAll(topicIds)
            this.force = true
        }
        return executeRequest(req)
    }

    open suspend fun readTopicReactions(
        chatId: Long,
        topicId: Int
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_readReactions().apply {
            peer = try {
                MessagesController.getInstance(currentAccount).getInputPeer(-chatId)
            } catch (_: Throwable) {
                TLRPC.TL_inputPeerChannel().apply { channel_id = chatId }
            }
            this.top_msg_id = topicId
        }
        return executeRequest(req)
    }
}
