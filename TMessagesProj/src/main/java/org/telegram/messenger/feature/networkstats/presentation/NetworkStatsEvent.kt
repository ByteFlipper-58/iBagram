package org.telegram.messenger.feature.networkstats.presentation

import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory

sealed class NetworkStatsEvent {
    data class SelectNetworkType(val networkType: NetworkType) : NetworkStatsEvent()
    data class ResetStats(val networkType: NetworkType) : NetworkStatsEvent()
    object Refresh : NetworkStatsEvent()
    data class IncrementTraffic(
        val networkType: NetworkType,
        val category: TrafficCategory,
        val sentBytes: Long = 0L,
        val receivedBytes: Long = 0L,
        val sentItems: Int = 0,
        val receivedItems: Int = 0
    ) : NetworkStatsEvent()
    data class IncrementCallsTime(
        val networkType: NetworkType,
        val seconds: Int
    ) : NetworkStatsEvent()
    object ClearMessage : NetworkStatsEvent()
}
