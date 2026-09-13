package org.telegram.messenger.feature.messaging.chatmeta.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories

/**
 * Remote data source encapsulating MTProto RPC operations for chat messages metadata:
 * reactions, extended media previews, and linked stories.
 */
open class ChatMetadataRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun loadReactions(
        peer: TLRPC.InputPeer,
        messageIds: List<Int>
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_getMessagesReactions()
        req.peer = peer
        for (id in messageIds) {
            req.id.add(id)
        }
        return executeRequest<TLRPC.Updates>(req)
    }

    open suspend fun loadExtendedMedia(
        peer: TLRPC.InputPeer,
        messageIds: List<Int>
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_getExtendedMedia()
        req.peer = peer
        for (id in messageIds) {
            req.id.add(id)
        }
        return executeRequest<TLRPC.Updates>(req)
    }

    open suspend fun loadStories(
        peer: TLRPC.InputPeer,
        storyIds: List<Int>
    ): Result<TL_stories.TL_stories_stories> {
        val req = TL_stories.TL_stories_getStoriesByID()
        req.peer = peer
        for (id in storyIds) {
            req.id.add(id)
        }
        return executeRequest<TL_stories.TL_stories_stories>(req)
    }
}
