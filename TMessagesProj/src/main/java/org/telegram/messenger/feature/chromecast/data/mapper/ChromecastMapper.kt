package org.telegram.messenger.feature.chromecast.data.mapper

import org.telegram.messenger.chromecast.ChromecastMedia
import org.telegram.messenger.chromecast.ChromecastMediaVariations
import org.telegram.messenger.feature.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.chromecast.domain.model.ChromecastStateModel

object ChromecastMapper {

    fun toDomainMedia(media: ChromecastMedia?): ChromecastMediaModel? {
        if (media == null) return null
        val title = try {
            media.mediaMetadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)
        } catch (_: Throwable) {
            null
        }
        val subtitle = try {
            media.mediaMetadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE)
        } catch (_: Throwable) {
            null
        }
        return ChromecastMediaModel(
            mimeType = media.mimeType ?: "",
            title = title,
            subtitle = subtitle,
            externalPath = media.externalPath,
            width = media.width,
            height = media.height
        )
    }

    fun toDomainMediaVariations(variations: ChromecastMediaVariations?): ChromecastMediaModel? {
        if (variations == null || variations.variationsCount == 0) return null
        return toDomainMedia(variations.getVariation(0))
    }

    fun toDomainState(
        isCasting: Boolean,
        deviceName: String?,
        currentMedia: ChromecastMediaModel?,
        isConnected: Boolean
    ): ChromecastStateModel = ChromecastStateModel(
        isCasting = isCasting,
        deviceName = deviceName,
        currentMedia = currentMedia,
        isConnected = isConnected
    )
}
