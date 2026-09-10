package org.telegram.messenger.feature.social.location.data.mapper

import android.location.Location
import org.telegram.messenger.LocationController
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.tgnet.TLRPC

/**
 * Mapper for location entities between legacy Telegram and domain representations.
 */
object LocationMapper {

    fun toDomain(sharing: LocationController.SharingLocationInfo?): LiveLocationSharingModel? {
        if (sharing == null) return null
        return LiveLocationSharingModel(
            dialogId = sharing.did,
            messageId = sharing.mid,
            stopTime = sharing.stopTime.toLong(),
            period = sharing.period,
            proximityMeters = sharing.proximityMeters,
            isActive = sharing.stopTime > (System.currentTimeMillis() / 1000L)
        )
    }

    fun toDomainList(sharings: List<LocationController.SharingLocationInfo>?): List<LiveLocationSharingModel> {
        if (sharings.isNullOrEmpty()) return emptyList()
        return sharings.mapNotNull { toDomain(it) }
    }

    fun toDomain(location: Location?): GeoPointModel? {
        if (location == null) return null
        return GeoPointModel(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy
        )
    }

    fun toPeerLocation(dialogId: Long, message: TLRPC.Message?): PeerLiveLocationModel? {
        if (message == null) return null
        val media = message.media
        val (lat, lon) = when (media) {
            is TLRPC.TL_messageMediaGeoLive -> {
                val geo = media.geo
                if (geo != null) Pair(geo.lat, geo._long) else null
            }
            is TLRPC.TL_messageMediaGeo -> {
                val geo = media.geo
                if (geo != null) Pair(geo.lat, geo._long) else null
            }
            else -> null
        } ?: return null

        val expiresIn = if (media is TLRPC.TL_messageMediaGeoLive) media.period else 0
        val fromId = when (val peer = message.from_id) {
            is TLRPC.TL_peerUser -> peer.user_id
            is TLRPC.TL_peerChannel -> peer.channel_id
            is TLRPC.TL_peerChat -> peer.chat_id
            else -> 0L
        }

        return PeerLiveLocationModel(
            messageId = message.id,
            dialogId = dialogId,
            fromId = fromId,
            geoPoint = GeoPointModel(latitude = lat, longitude = lon),
            date = message.date.toLong(),
            expiresIn = expiresIn,
            unread = message.unread
        )
    }

    fun toPeerLocationList(dialogId: Long, messages: List<TLRPC.Message>?): List<PeerLiveLocationModel> {
        if (messages.isNullOrEmpty()) return emptyList()
        return messages.mapNotNull { toPeerLocation(dialogId, it) }
    }
}
