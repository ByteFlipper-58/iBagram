package org.telegram.messenger.feature.network.networkstats.data.mapper

import org.telegram.messenger.StatsController
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficItemModel
import org.telegram.messenger.feature.network.networkstats.domain.usecase.CalculateMessagesTrafficUseCase

object NetworkStatsMapper {

    private val calculateMessagesTraffic = CalculateMessagesTrafficUseCase()

    fun toLegacyNetworkType(type: NetworkType): Int {
        return when (type) {
            NetworkType.MOBILE -> StatsController.TYPE_MOBILE
            NetworkType.WIFI -> StatsController.TYPE_WIFI
            NetworkType.ROAMING -> StatsController.TYPE_ROAMING
        }
    }

    fun toNetworkType(legacyType: Int): NetworkType {
        return when (legacyType) {
            StatsController.TYPE_MOBILE -> NetworkType.MOBILE
            StatsController.TYPE_WIFI -> NetworkType.WIFI
            StatsController.TYPE_ROAMING -> NetworkType.ROAMING
            else -> NetworkType.MOBILE
        }
    }

    fun toLegacyTrafficCategory(category: TrafficCategory): Int {
        return when (category) {
            TrafficCategory.CALLS -> StatsController.TYPE_CALLS
            TrafficCategory.MESSAGES -> StatsController.TYPE_MESSAGES
            TrafficCategory.VIDEOS -> StatsController.TYPE_VIDEOS
            TrafficCategory.AUDIOS -> StatsController.TYPE_AUDIOS
            TrafficCategory.PHOTOS -> StatsController.TYPE_PHOTOS
            TrafficCategory.FILES -> StatsController.TYPE_FILES
            TrafficCategory.TOTAL -> StatsController.TYPE_TOTAL
            TrafficCategory.MUSIC -> StatsController.TYPE_MUSIC
        }
    }

    fun toTrafficCategory(legacyDataType: Int): TrafficCategory {
        return when (legacyDataType) {
            StatsController.TYPE_CALLS -> TrafficCategory.CALLS
            StatsController.TYPE_MESSAGES -> TrafficCategory.MESSAGES
            StatsController.TYPE_VIDEOS -> TrafficCategory.VIDEOS
            StatsController.TYPE_AUDIOS -> TrafficCategory.AUDIOS
            StatsController.TYPE_PHOTOS -> TrafficCategory.PHOTOS
            StatsController.TYPE_FILES -> TrafficCategory.FILES
            StatsController.TYPE_TOTAL -> TrafficCategory.TOTAL
            StatsController.TYPE_MUSIC -> TrafficCategory.MUSIC
            else -> TrafficCategory.TOTAL
        }
    }

    fun buildSummary(
        networkType: NetworkType,
        statsController: StatsController?
    ): NetworkStatsSummaryModel {
        if (statsController == null) {
            return NetworkStatsSummaryModel(
                networkType = networkType,
                items = TrafficCategory.values().associateWith { TrafficItemModel(category = it) },
                callsTotalTimeSec = 0,
                resetStatsDateMs = 0L
            )
        }

        val legacyNet = toLegacyNetworkType(networkType)
        val rawItems = mutableMapOf<TrafficCategory, TrafficItemModel>()

        for (category in TrafficCategory.values()) {
            val legacyCat = toLegacyTrafficCategory(category)
            val sent = statsController.getSentBytesCount(legacyNet, legacyCat)
            val rec = statsController.getReceivedBytesCount(legacyNet, legacyCat)
            val sentCnt = statsController.getSentItemsCount(legacyNet, legacyCat)
            val recCnt = statsController.getRecivedItemsCount(legacyNet, legacyCat)

            rawItems[category] = TrafficItemModel(
                category = category,
                sentBytes = sent,
                receivedBytes = rec,
                sentItems = sentCnt,
                receivedItems = recCnt
            )
        }

        // StatsController internally calculates messages as total - other categories.
        // Let's ensure consistency with CalculateMessagesTrafficUseCase
        val computedMessages = calculateMessagesTraffic(rawItems)
        rawItems[TrafficCategory.MESSAGES] = computedMessages

        val callsTime = statsController.getCallsTotalTime(legacyNet)
        val resetDate = statsController.getResetStatsDate(legacyNet)

        return NetworkStatsSummaryModel(
            networkType = networkType,
            items = rawItems,
            callsTotalTimeSec = callsTime,
            resetStatsDateMs = resetDate
        )
    }
}
