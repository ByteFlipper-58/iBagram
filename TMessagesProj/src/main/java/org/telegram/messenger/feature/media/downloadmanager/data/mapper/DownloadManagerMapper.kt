package org.telegram.messenger.feature.media.downloadmanager.data.mapper

import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.PeerTypePreset
import java.util.EnumSet
import java.util.Locale

object DownloadManagerMapper {

    const val AUTODOWNLOAD_TYPE_PHOTO = 1
    const val AUTODOWNLOAD_TYPE_AUDIO = 2
    const val AUTODOWNLOAD_TYPE_VIDEO = 4
    const val AUTODOWNLOAD_TYPE_DOCUMENT = 8

    fun encodeMediaMask(types: Set<AutoDownloadMediaType>): Int {
        var mask = 0
        for (type in types) {
            mask = when (type) {
                AutoDownloadMediaType.PHOTO -> mask or AUTODOWNLOAD_TYPE_PHOTO
                AutoDownloadMediaType.AUDIO -> mask or AUTODOWNLOAD_TYPE_AUDIO
                AutoDownloadMediaType.VIDEO -> mask or AUTODOWNLOAD_TYPE_VIDEO
                AutoDownloadMediaType.DOCUMENT -> mask or AUTODOWNLOAD_TYPE_DOCUMENT
            }
        }
        return mask
    }

    fun decodeMediaMask(mask: Int): Set<AutoDownloadMediaType> {
        val set = EnumSet.noneOf(AutoDownloadMediaType::class.java)
        if ((mask and AUTODOWNLOAD_TYPE_PHOTO) != 0) set.add(AutoDownloadMediaType.PHOTO)
        if ((mask and AUTODOWNLOAD_TYPE_AUDIO) != 0) set.add(AutoDownloadMediaType.AUDIO)
        if ((mask and AUTODOWNLOAD_TYPE_VIDEO) != 0) set.add(AutoDownloadMediaType.VIDEO)
        if ((mask and AUTODOWNLOAD_TYPE_DOCUMENT) != 0) set.add(AutoDownloadMediaType.DOCUMENT)
        return set
    }

    fun parsePresetString(str: String): DownloadPresetModel {
        val parts = str.split("_")
        if (parts.size < 11) {
            return DownloadPresetModel()
        }

        val masksMap = mutableMapOf<PeerTypePreset, Set<AutoDownloadMediaType>>()
        val presets = PeerTypePreset.values()
        for (i in 0 until minOf(4, presets.size)) {
            val maskInt = parts[i].toIntOrNull() ?: 0
            masksMap[presets[i]] = decodeMediaMask(maskInt)
        }

        val sizesMap = mutableMapOf<AutoDownloadMediaType, Long>()
        sizesMap[AutoDownloadMediaType.PHOTO] = parts.getOrNull(4)?.toLongOrNull() ?: (500L * 1024L)
        sizesMap[AutoDownloadMediaType.VIDEO] = parts.getOrNull(5)?.toLongOrNull() ?: (10L * 1024L * 1024L)
        sizesMap[AutoDownloadMediaType.DOCUMENT] = parts.getOrNull(6)?.toLongOrNull() ?: (3L * 1024L * 1024L)
        sizesMap[AutoDownloadMediaType.AUDIO] = parts.getOrNull(7)?.toLongOrNull() ?: (512L * 1024L)

        val preloadVideo = parts.getOrNull(8) == "1"
        val preloadMusic = parts.getOrNull(9) == "1"
        val enabled = parts.getOrNull(10) == "1"
        val lessCallData = parts.getOrNull(11) == "1"
        val maxBitrate = parts.getOrNull(12)?.toIntOrNull() ?: 1000
        val preloadStories = parts.getOrNull(13) == "1"

        return DownloadPresetModel(
            mask = masksMap,
            maxSizes = sizesMap,
            preloadVideo = preloadVideo,
            preloadMusic = preloadMusic,
            preloadStories = preloadStories,
            lessCallData = lessCallData,
            maxVideoBitrate = maxBitrate,
            enabled = enabled
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        val kb = 1024L
        val mb = kb * 1024L

        return when {
            bytesPerSec >= mb -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toDouble() / mb)
            bytesPerSec >= kb -> String.format(Locale.US, "%.1f KB/s", bytesPerSec.toDouble() / kb)
            else -> "$bytesPerSec B/s"
        }
    }
}
