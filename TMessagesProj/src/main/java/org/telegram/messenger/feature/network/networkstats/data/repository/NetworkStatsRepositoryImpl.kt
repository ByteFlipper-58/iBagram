package org.telegram.messenger.feature.network.networkstats.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.feature.network.networkstats.data.datasource.NetworkStatsLocalDataSource
import org.telegram.messenger.feature.network.networkstats.data.datasource.NetworkStatsRemoteDataSource
import org.telegram.messenger.feature.network.networkstats.data.mapper.NetworkStatsMapper
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficItemModel
import org.telegram.messenger.feature.network.networkstats.domain.repository.NetworkStatsRepository
import org.telegram.messenger.feature.network.networkstats.domain.usecase.CalculateMessagesTrafficUseCase

class NetworkStatsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: NetworkStatsLocalDataSource,
    private val remoteDataSource: NetworkStatsRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NetworkStatsRepository {

    private val calculateMessagesTraffic = CalculateMessagesTrafficUseCase()

    private val _statsFlow = MutableStateFlow<Map<NetworkType, NetworkStatsSummaryModel>>(
        NetworkType.values().associateWith { createEmptySummary(it) }
    )

    init {
        loadInitialStats()
    }

    private fun createEmptySummary(type: NetworkType): NetworkStatsSummaryModel {
        return NetworkStatsSummaryModel(
            networkType = type,
            items = TrafficCategory.values().associateWith { TrafficItemModel(category = it) },
            callsTotalTimeSec = 0,
            resetStatsDateMs = System.currentTimeMillis()
        )
    }

    private fun buildSummaryFromLocal(networkType: NetworkType): NetworkStatsSummaryModel {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val rawItems = mutableMapOf<TrafficCategory, TrafficItemModel>()

        for (category in TrafficCategory.values()) {
            val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)
            val sent = localDataSource.getSentBytesCount(legacyNet, legacyCat)
            val rec = localDataSource.getReceivedBytesCount(legacyNet, legacyCat)
            val sentCnt = localDataSource.getSentItemsCount(legacyNet, legacyCat)
            val recCnt = localDataSource.getReceivedItemsCount(legacyNet, legacyCat)

            rawItems[category] = TrafficItemModel(
                category = category,
                sentBytes = sent,
                receivedBytes = rec,
                sentItems = sentCnt,
                receivedItems = recCnt
            )
        }

        val computedMessages = calculateMessagesTraffic(rawItems)
        rawItems[TrafficCategory.MESSAGES] = computedMessages

        val callsTime = localDataSource.getCallsTotalTime(legacyNet)
        val resetDate = localDataSource.getResetStatsDate(legacyNet)

        return NetworkStatsSummaryModel(
            networkType = networkType,
            items = rawItems,
            callsTotalTimeSec = callsTime,
            resetStatsDateMs = resetDate
        )
    }

    private fun loadInitialStats() {
        val map = NetworkType.values().associateWith { netType ->
            buildSummaryFromLocal(netType)
        }
        _statsFlow.value = map
    }

    override fun observeStats(networkType: NetworkType): Flow<NetworkStatsSummaryModel> {
        return _statsFlow.asStateFlow()
            .map { it[networkType] ?: createEmptySummary(networkType) }
            .distinctUntilChanged()
    }

    override fun observeAllStats(): Flow<Map<NetworkType, NetworkStatsSummaryModel>> {
        return _statsFlow.asStateFlow()
    }

    override suspend fun getStats(networkType: NetworkType): NetworkStatsSummaryModel = withContext(ioDispatcher) {
        val summary = buildSummaryFromLocal(networkType)
        val updated = _statsFlow.value.toMutableMap()
        updated[networkType] = summary
        _statsFlow.value = updated
        summary
    }

    override suspend fun getAllStats(): Map<NetworkType, NetworkStatsSummaryModel> = withContext(ioDispatcher) {
        val map = NetworkType.values().associateWith { netType ->
            buildSummaryFromLocal(netType)
        }
        _statsFlow.value = map
        map
    }

    override suspend fun incrementSentBytes(
        networkType: NetworkType,
        category: TrafficCategory,
        bytes: Long
    ) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)
        localDataSource.incrementSentBytesCount(legacyNet, legacyCat, bytes)
        refreshInternal(networkType)
    }

    override suspend fun incrementReceivedBytes(
        networkType: NetworkType,
        category: TrafficCategory,
        bytes: Long
    ) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)
        localDataSource.incrementReceivedBytesCount(legacyNet, legacyCat, bytes)
        refreshInternal(networkType)
    }

    override suspend fun incrementSentItems(
        networkType: NetworkType,
        category: TrafficCategory,
        count: Int
    ) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)
        localDataSource.incrementSentItemsCount(legacyNet, legacyCat, count)
        refreshInternal(networkType)
    }

    override suspend fun incrementReceivedItems(
        networkType: NetworkType,
        category: TrafficCategory,
        count: Int
    ) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)
        localDataSource.incrementReceivedItemsCount(legacyNet, legacyCat, count)
        refreshInternal(networkType)
    }

    override suspend fun incrementCallsTotalTime(
        networkType: NetworkType,
        seconds: Int
    ) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        localDataSource.incrementTotalCallsTime(legacyNet, seconds)
        refreshInternal(networkType)
    }

    override suspend fun resetStats(networkType: NetworkType) = withContext(ioDispatcher) {
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        localDataSource.resetStats(legacyNet)
        remoteDataSource.resetRemoteStats()
        refreshInternal(networkType)
    }

    override suspend fun refreshStats(): Unit = withContext(ioDispatcher) {
        remoteDataSource.syncNetworkStats()
        getAllStats()
        Unit
    }

    private fun refreshInternal(networkType: NetworkType) {
        val summary = buildSummaryFromLocal(networkType)
        val updated = _statsFlow.value.toMutableMap()
        updated[networkType] = summary
        _statsFlow.value = updated
    }
}
