package org.telegram.messenger.feature.messaging.savedmessages.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source for Saved Messages operations via MTProto RPC requests.
 */
open class SavedMessagesRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches a slice of saved dialogs from Telegram MTProto servers.
     */
    open suspend fun getSavedDialogs(
        offsetId: Int,
        offsetDate: Int,
        offsetPeer: TLRPC.InputPeer,
        limit: Int,
        hash: Long
    ): Result<TLRPC.messages_SavedDialogs> {
        val req = TLRPC.TL_messages_getSavedDialogs().apply {
            this.offset_id = offsetId
            this.offset_date = offsetDate
            this.offset_peer = offsetPeer
            this.limit = limit
            this.hash = hash
        }
        return executeRequest(req)
    }

    /**
     * Synchronizes reordered pinned saved dialogs with Telegram MTProto servers.
     */
    open suspend fun reorderPinnedSavedDialogs(order: List<Long>): Result<Boolean> {
        val req = TLRPC.TL_messages_reorderPinnedSavedDialogs().apply {
            this.force = true
            for (did in order) {
                val inputPeer = MessagesController.getInstance(currentAccount).getInputPeer(did)
                if (inputPeer != null) {
                    val dialogPeer = TLRPC.TL_inputDialogPeer().apply {
                        this.peer = inputPeer
                    }
                    this.order.add(dialogPeer)
                }
            }
        }
        return executeRequest<TLRPC.Bool>(req).map { it is TLRPC.TL_boolTrue }
    }
}
