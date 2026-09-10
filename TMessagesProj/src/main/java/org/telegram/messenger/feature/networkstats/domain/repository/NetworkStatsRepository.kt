package org.telegram.messenger.feature.networkstats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory

interface NetworkStatsRepository {
    fun observeStats(networkType: NetworkType): Flow<NetworkStatsSummaryModel>
    fun observeAllStats(): Flow<Map<NetworkType, NetworkStatsSummaryModel>>
    suspend fun getStats(networkType: NetworkType): NetworkStatsSummaryModel
    suspend fun getAllStats(): Map<NetworkType, NetworkStatsSummaryModel>
    suspend fun incrementSentBytes(networkType: NetworkType, category: TrafficCategory, bytes: Long)
    suspend fun incrementReceivedBytes(networkType: NetworkType, category: TrafficCategory, bytes: Long)
    suspend fun incrementSentItems(networkType: NetworkType, category: TrafficCategory, count: Int = 1)
    suspend fun incrementReceivedItems(networkType: NetworkType, category: TrafficCategory, count: Int = 1)
    suspend fun incrementCallsTotalTime(networkType: NetworkType, seconds: Int)
    suspend fun resetStats(networkType: NetworkType)
    suspend fun refreshStats()
}
