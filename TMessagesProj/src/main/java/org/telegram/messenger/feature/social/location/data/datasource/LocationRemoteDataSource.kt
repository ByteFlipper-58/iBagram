package org.telegram.messenger.feature.social.location.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Remote data source executing MTProto RPC requests for live locations, updates, and read confirmations.
 */
open class LocationRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches recent live locations sent by participants in the specified [peer].
     */
    open suspend fun getRecentLocations(
        peer: TLRPC.InputPeer,
        limit: Int = 100,
        hash: Long = 0L
    ): Result<TLRPC.messages_Messages> {
        val req = TLRPC.TL_messages_getRecentLocations().apply {
            this.peer = peer
            this.limit = limit
            this.hash = hash
        }
        return executeRequest(req)
    }

    /**
     * Stops sharing a live location in [peer] for the given [messageId].
     */
    open suspend fun stopLiveLocation(
        peer: TLRPC.InputPeer,
        messageId: Int
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_editMessage().apply {
            this.peer = peer
            this.id = messageId
            this.flags = this.flags or 16384
            this.media = TLRPC.TL_inputMediaGeoLive().apply {
                this.stopped = true
                this.geo_point = TLRPC.TL_inputGeoPointEmpty()
            }
        }
        return executeRequest(req)
    }

    /**
     * Edits/updates live location coordinates and proximity radius for [messageId] in [peer].
     */
    open suspend fun editLiveLocation(
        peer: TLRPC.InputPeer,
        messageId: Int,
        lat: Double,
        lon: Double,
        period: Int,
        proximityRadius: Int = 0
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_messages_editMessage().apply {
            this.peer = peer
            this.id = messageId
            this.flags = this.flags or 16384
            this.media = TLRPC.TL_inputMediaGeoLive().apply {
                this.stopped = false
                this.period = period
                this.proximity_notification_radius = proximityRadius
                this.geo_point = TLRPC.TL_inputGeoPoint().apply {
                    this.lat = lat
                    this._long = lon
                }
            }
        }
        return executeRequest(req)
    }

    /**
     * Marks peer live location messages as read on the server.
     */
    open suspend fun markLiveLocationsAsRead(
        messageIds: List<Int>,
        isChannel: Boolean = false,
        channel: TLRPC.InputChannel? = null
    ): Result<Boolean> {
        if (messageIds.isEmpty()) return Result.success(true)

        return if (isChannel && channel != null) {
            val req = TLRPC.TL_channels_readMessageContents().apply {
                this.channel = channel
                this.id.addAll(messageIds)
            }
            val res = executeRequest<TLRPC.Bool>(req)
            when (res) {
                is Result.Success -> Result.Success(true)
                is Result.Failure -> Result.Failure(res.error)
            }
        } else {
            val req = TLRPC.TL_messages_readMessageContents().apply {
                this.id.addAll(messageIds)
            }
            val res = executeRequest<TLRPC.TL_messages_affectedMessages>(req)
            when (res) {
                is Result.Success -> Result.Success(true)
                is Result.Failure -> Result.Failure(res.error)
            }
        }
    }
}
