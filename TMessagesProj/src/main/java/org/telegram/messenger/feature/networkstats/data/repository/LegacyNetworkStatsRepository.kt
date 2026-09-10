package org.telegram.messenger.feature.networkstats.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.StatsController
import org.telegram.messenger.feature.networkstats.data.mapper.NetworkStatsMapper
import org.telegram.messenger.feature.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.networkstats.domain.model.TrafficItemModel
import org.telegram.messenger.feature.networkstats.domain.repository.NetworkStatsRepository

class LegacyNetworkStatsRepository(
    private val currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val controllerProvider: () -> StatsController? = {
        runCatching { StatsController.getInstance(currentAccount) }.getOrNull()
    }
) : NetworkStatsRepository {

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

    private fun loadInitialStats() {
        val controller = controllerProvider()
        if (controller != null) {
            val map = NetworkType.values().associateWith { netType ->
                NetworkStatsMapper.buildSummary(netType, controller)
            }
            _statsFlow.value = map
        }
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
        val controller = controllerProvider()
        if (controller != null) {
            val summary = NetworkStatsMapper.buildSummary(networkType, controller)
            val updated = _statsFlow.value.toMutableMap()
            updated[networkType] = summary
            _statsFlow.value = updated
            summary
        } else {
            _statsFlow.value[networkType] ?: createEmptySummary(networkType)
        }
    }

    override suspend fun getAllStats(): Map<NetworkType, NetworkStatsSummaryModel> = withContext(ioDispatcher) {
        val controller = controllerProvider()
        if (controller != null) {
            val map = NetworkType.values().associateWith { netType ->
                NetworkStatsMapper.buildSummary(netType, controller)
            }
            _statsFlow.value = map
            map
        } else {
            _statsFlow.value
        }
    }

    override suspend fun incrementSentBytes(
        networkType: NetworkType,
        category: TrafficCategory,
        bytes: Long
    ) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)

        if (controller != null) {
            controller.incrementSentBytesCount(legacyNet, legacyCat, bytes)
            refreshInternal(networkType)
        } else {
            updateInMemory(networkType, category) { oldItem ->
                oldItem.copy(sentBytes = oldItem.sentBytes + bytes)
            }
        }
    }

    override suspend fun incrementReceivedBytes(
        networkType: NetworkType,
        category: TrafficCategory,
        bytes: Long
    ) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)

        if (controller != null) {
            controller.incrementReceivedBytesCount(legacyNet, legacyCat, bytes)
            refreshInternal(networkType)
        } else {
            updateInMemory(networkType, category) { oldItem ->
                oldItem.copy(receivedBytes = oldItem.receivedBytes + bytes)
            }
        }
    }

    override suspend fun incrementSentItems(
        networkType: NetworkType,
        category: TrafficCategory,
        count: Int
    ) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)

        if (controller != null) {
            controller.incrementSentItemsCount(legacyNet, legacyCat, count)
            refreshInternal(networkType)
        } else {
            updateInMemory(networkType, category) { oldItem ->
                oldItem.copy(sentItems = oldItem.sentItems + count)
            }
        }
    }

    override suspend fun incrementReceivedItems(
        networkType: NetworkType,
        category: TrafficCategory,
        count: Int
    ) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)
        val legacyCat = NetworkStatsMapper.toLegacyTrafficCategory(category)

        if (controller != null) {
            controller.incrementReceivedItemsCount(legacyNet, legacyCat, count)
            refreshInternal(networkType)
        } else {
            updateInMemory(networkType, category) { oldItem ->
                oldItem.copy(receivedItems = oldItem.receivedItems + count)
            }
        }
    }

    override suspend fun incrementCallsTotalTime(
        networkType: NetworkType,
        seconds: Int
    ) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)

        if (controller != null) {
            controller.incrementTotalCallsTime(legacyNet, seconds)
            refreshInternal(networkType)
        } else {
            val currentMap = _statsFlow.value.toMutableMap()
            val summary = currentMap[networkType] ?: createEmptySummary(networkType)
            currentMap[networkType] = summary.copy(
                callsTotalTimeSec = summary.callsTotalTimeSec + seconds
            )
            _statsFlow.value = currentMap
        }
    }

    override suspend fun resetStats(networkType: NetworkType) = withContext(ioDispatcher) {
        val controller = controllerProvider()
        val legacyNet = NetworkStatsMapper.toLegacyNetworkType(networkType)

        if (controller != null) {
            controller.resetStats(legacyNet)
            refreshInternal(networkType)
        } else {
            val currentMap = _statsFlow.value.toMutableMap()
            currentMap[networkType] = createEmptySummary(networkType).copy(
                resetStatsDateMs = System.currentTimeMillis()
            )
            _statsFlow.value = currentMap
        }
    }

    override suspend fun refreshStats(): Unit = withContext(ioDispatcher) {
        getAllStats()
        Unit
    }

    private fun refreshInternal(networkType: NetworkType) {
        val controller = controllerProvider() ?: return
        val summary = NetworkStatsMapper.buildSummary(networkType, controller)
        val updated = _statsFlow.value.toMutableMap()
        updated[networkType] = summary
        _statsFlow.value = updated
    }

    private fun updateInMemory(
        networkType: NetworkType,
        category: TrafficCategory,
        transform: (TrafficItemModel) -> TrafficItemModel
    ) {
        val currentMap = _statsFlow.value.toMutableMap()
        val summary = currentMap[networkType] ?: createEmptySummary(networkType)
        val updatedItems = summary.items.toMutableMap()
        val item = updatedItems[category] ?: TrafficItemModel(category = category)
        updatedItems[category] = transform(item)
        currentMap[networkType] = summary.copy(items = updatedItems)
        _statsFlow.value = currentMap
    }
}
