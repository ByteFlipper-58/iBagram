package org.telegram.messenger.feature.datastorage.data.mapper

import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.DownloadController
import org.telegram.messenger.StatsController
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.model.StorageUsageModel

object DataStorageMapper {

    fun mapNetworkUsage(statsController: StatsController, networkType: NetworkUsageType): NetworkUsageModel {
        val netType = when (networkType) {
            NetworkUsageType.MOBILE -> StatsController.TYPE_MOBILE
            NetworkUsageType.WIFI -> StatsController.TYPE_WIFI
            NetworkUsageType.ROAMING -> StatsController.TYPE_ROAMING
        }

        return NetworkUsageModel(
            networkType = networkType,
            bytesSent = statsController.getSentBytesCount(netType, StatsController.TYPE_TOTAL),
            bytesReceived = statsController.getReceivedBytesCount(netType, StatsController.TYPE_TOTAL),
            messagesSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_MESSAGES),
            messagesReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_MESSAGES),
            photosSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_PHOTOS),
            photosReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_PHOTOS),
            videosSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_VIDEOS),
            videosReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_VIDEOS),
            audioSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_AUDIOS),
            audioReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_AUDIOS),
            filesSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_FILES),
            filesReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_FILES),
            callsSentBytes = statsController.getSentBytesCount(netType, StatsController.TYPE_CALLS),
            callsReceivedBytes = statsController.getReceivedBytesCount(netType, StatsController.TYPE_CALLS),
            callsTotalTimeSeconds = statsController.getCallsTotalTime(netType),
            resetDate = statsController.getResetStatsDate(netType)
        )
    }

    fun mapAutoDownloadPreset(
        preset: DownloadController.Preset,
        type: AutoDownloadNetworkType
    ): AutoDownloadPresetModel {
        val hasPhotos = (preset.mask[DownloadController.PRESET_NUM_PM] and DownloadController.AUTODOWNLOAD_TYPE_PHOTO) != 0
        val hasVideos = (preset.mask[DownloadController.PRESET_NUM_PM] and DownloadController.AUTODOWNLOAD_TYPE_VIDEO) != 0
        val hasDocuments = (preset.mask[DownloadController.PRESET_NUM_PM] and DownloadController.AUTODOWNLOAD_TYPE_DOCUMENT) != 0

        return AutoDownloadPresetModel(
            networkType = type,
            isEnabled = preset.isEnabled,
            downloadPhotos = hasPhotos,
            downloadVideos = hasVideos,
            downloadDocuments = hasDocuments,
            maxVideoSize = preset.sizes[DownloadController.PRESET_SIZE_NUM_VIDEO],
            maxDocumentSize = preset.sizes[DownloadController.PRESET_SIZE_NUM_DOCUMENT],
            preloadVideo = preset.preloadVideo,
            preloadMusic = preset.preloadMusic,
            preloadStories = preset.preloadStories,
            lessCallData = preset.lessCallData
        )
    }

    fun applyPresetModel(model: AutoDownloadPresetModel, preset: DownloadController.Preset) {
        preset.enabled = model.isEnabled
        preset.preloadVideo = model.preloadVideo
        preset.preloadMusic = model.preloadMusic
        preset.preloadStories = model.preloadStories
        preset.lessCallData = model.lessCallData
        preset.sizes[DownloadController.PRESET_SIZE_NUM_VIDEO] = model.maxVideoSize
        preset.sizes[DownloadController.PRESET_SIZE_NUM_DOCUMENT] = model.maxDocumentSize

        for (i in 0 until 4) {
            var mask = 0
            if (model.downloadPhotos) mask = mask or DownloadController.AUTODOWNLOAD_TYPE_PHOTO
            if (model.downloadVideos) mask = mask or DownloadController.AUTODOWNLOAD_TYPE_VIDEO
            if (model.downloadDocuments) mask = mask or DownloadController.AUTODOWNLOAD_TYPE_DOCUMENT
            preset.mask[i] = mask
        }
    }

    fun mapKeepMediaSettings(cacheByChatsController: CacheByChatsController): KeepMediaSettingsModel {
        return KeepMediaSettingsModel(
            keepMediaUser = cacheByChatsController.getKeepMedia(CacheByChatsController.KEEP_MEDIA_TYPE_USER),
            keepMediaGroup = cacheByChatsController.getKeepMedia(CacheByChatsController.KEEP_MEDIA_TYPE_GROUP),
            keepMediaChannel = cacheByChatsController.getKeepMedia(CacheByChatsController.KEEP_MEDIA_TYPE_CHANNEL),
            keepMediaStories = cacheByChatsController.getKeepMedia(CacheByChatsController.KEEP_MEDIA_TYPE_STORIES)
        )
    }
}
