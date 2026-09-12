package org.telegram.messenger.feature.social.boosts.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories

/**
 * Remote data source executing MTProto RPC requests for channel and chat boosts operations.
 */
open class BoostsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches current boosts status for peer from MTProto server.
     */
    open suspend fun getBoostsStatus(peer: TLRPC.InputPeer): Result<TL_stories.TL_premium_boostsStatus> {
        val req = TL_stories.TL_premium_getBoostsStatus().apply {
            this.peer = peer
        }
        return executeRequest(req)
    }

    /**
     * Fetches caller's active and available boost slots from MTProto server.
     */
    open suspend fun getMyBoosts(): Result<TL_stories.TL_premium_myBoosts> {
        val req = TL_stories.TL_premium_getMyBoosts()
        return executeRequest(req)
    }

    /**
     * Applies boosts to a target peer using specified slots.
     */
    open suspend fun applyBoost(peer: TLRPC.InputPeer, slots: List<Int>): Result<TL_stories.TL_premium_myBoosts> {
        val req = TL_stories.TL_premium_applyBoost().apply {
            this.peer = peer
            this.flags = this.flags or 1
            this.slots.addAll(slots)
        }
        return executeRequest(
            request = req,
            flags = ConnectionsManager.RequestFlagInvokeAfter or ConnectionsManager.RequestFlagFailOnServerErrors
        )
    }
}
