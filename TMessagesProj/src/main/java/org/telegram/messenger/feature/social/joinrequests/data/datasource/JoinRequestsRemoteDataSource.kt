package org.telegram.messenger.feature.social.joinrequests.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source executing MTProto RPC requests for chat join requests and invite importers.
 */
open class JoinRequestsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches chat invite importers or pending join requests from MTProto server.
     */
    open suspend fun getChatInviteImporters(
        peer: TLRPC.InputPeer,
        requested: Boolean = true,
        limit: Int = 30,
        query: String? = null,
        offsetUser: TLRPC.InputUser = TLRPC.TL_inputUserEmpty(),
        offsetDate: Int = 0
    ): Result<TLRPC.TL_messages_chatInviteImporters> {
        val req = TLRPC.TL_messages_getChatInviteImporters().apply {
            this.peer = peer
            this.requested = requested
            this.limit = limit
            if (!query.isNullOrEmpty()) {
                this.q = query
                this.flags = this.flags or 4
            }
            this.offset_user = offsetUser
            this.offset_date = offsetDate
        }
        return executeRequest(req)
    }

    /**
     * Approves or dismisses a single chat join request.
     */
    open suspend fun hideChatJoinRequest(
        peer: TLRPC.InputPeer,
        inputUser: TLRPC.InputUser,
        approved: Boolean
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_hideChatJoinRequest().apply {
            this.approved = approved
            this.peer = peer
            this.user_id = inputUser
        }
        return executeRequest(req)
    }

    /**
     * Approves or dismisses all pending chat join requests, optionally filtered by invite link.
     */
    open suspend fun hideAllChatJoinRequests(
        peer: TLRPC.InputPeer,
        inviteLink: String? = null,
        approved: Boolean
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_hideAllChatJoinRequests().apply {
            this.approved = approved
            this.peer = peer
            if (!inviteLink.isNullOrEmpty()) {
                this.link = inviteLink
                this.flags = this.flags or 1
            }
        }
        return executeRequest(req)
    }
}
