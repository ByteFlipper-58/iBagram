package org.telegram.messenger.feature.networkstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.networkstats.data.mapper.NetworkStatsMapper
import org.telegram.messenger.feature.networkstats.data.repository.LegacyNetworkStatsRepository
import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.networkstats.domain.model.TrafficItemModel
import org.telegram.messenger.feature.networkstats.domain.usecase.CalculateMessagesTrafficUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.FormatCallsDurationUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.FormatTrafficBytesUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.GetAllNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.GetNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementCallsTimeUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementTrafficBytesUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementTrafficItemsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ObserveAllNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ObserveNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.RefreshNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ResetNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.presentation.NetworkStatsEvent
import org.telegram.messenger.feature.networkstats.presentation.NetworkStatsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatsDomainTest {

    private lateinit var repository: LegacyNetworkStatsRepository
    private lateinit var observeStatsUseCase: ObserveNetworkStatsUseCase
    private lateinit var observeAllStatsUseCase: ObserveAllNetworkStatsUseCase
    private lateinit var getStatsUseCase: GetNetworkStatsUseCase
    private lateinit var getAllStatsUseCase: GetAllNetworkStatsUseCase
    private lateinit var incrementTrafficBytesUseCase: IncrementTrafficBytesUseCase
    private lateinit var incrementTrafficItemsUseCase: IncrementTrafficItemsUseCase
    private lateinit var incrementCallsTimeUseCase: IncrementCallsTimeUseCase
    private lateinit var resetStatsUseCase: ResetNetworkStatsUseCase
    private lateinit var refreshStatsUseCase: RefreshNetworkStatsUseCase
    private lateinit var calculateMessagesTrafficUseCase: CalculateMessagesTrafficUseCase
    private lateinit var formatTrafficBytesUseCase: FormatTrafficBytesUseCase
    private lateinit var formatCallsDurationUseCase: FormatCallsDurationUseCase

    @Before
    fun setUp() {
        // Use headless repository with controllerProvider returning null for pure unit tests
        repository = LegacyNetworkStatsRepository(
            currentAccount = 0,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
            controllerProvider = { null }
        )
        observeStatsUseCase = ObserveNetworkStatsUseCase(repository)
        observeAllStatsUseCase = ObserveAllNetworkStatsUseCase(repository)
        getStatsUseCase = GetNetworkStatsUseCase(repository)
        getAllStatsUseCase = GetAllNetworkStatsUseCase(repository)
        incrementTrafficBytesUseCase = IncrementTrafficBytesUseCase(repository)
        incrementTrafficItemsUseCase = IncrementTrafficItemsUseCase(repository)
        incrementCallsTimeUseCase = IncrementCallsTimeUseCase(repository)
        resetStatsUseCase = ResetNetworkStatsUseCase(repository)
        refreshStatsUseCase = RefreshNetworkStatsUseCase(repository)
        calculateMessagesTrafficUseCase = CalculateMessagesTrafficUseCase()
        formatTrafficBytesUseCase = FormatTrafficBytesUseCase()
        formatCallsDurationUseCase = FormatCallsDurationUseCase()
    }

    @Test
    fun testCalculateMessagesTrafficUseCase() {
        val rawItems = mutableMapOf<TrafficCategory, TrafficItemModel>()
        rawItems[TrafficCategory.TOTAL] = TrafficItemModel(TrafficCategory.TOTAL, sentBytes = 10000L, receivedBytes = 20000L)
        rawItems[TrafficCategory.FILES] = TrafficItemModel(TrafficCategory.FILES, sentBytes = 2000L, receivedBytes = 4000L)
        rawItems[TrafficCategory.AUDIOS] = TrafficItemModel(TrafficCategory.AUDIOS, sentBytes = 1000L, receivedBytes = 2000L)
        rawItems[TrafficCategory.VIDEOS] = TrafficItemModel(TrafficCategory.VIDEOS, sentBytes = 3000L, receivedBytes = 5000L)
        rawItems[TrafficCategory.PHOTOS] = TrafficItemModel(TrafficCategory.PHOTOS, sentBytes = 1500L, receivedBytes = 3000L)
        rawItems[TrafficCategory.MUSIC] = TrafficItemModel(TrafficCategory.MUSIC, sentBytes = 500L, receivedBytes = 1000L)
        rawItems[TrafficCategory.MESSAGES] = TrafficItemModel(TrafficCategory.MESSAGES, sentItems = 42, receivedItems = 84)

        // total sent = 10000, media sum = 2000 + 1000 + 3000 + 1500 + 500 = 8000 -> messages sent = 2000
        // total rec = 20000, media sum = 4000 + 2000 + 5000 + 3000 + 1000 = 15000 -> messages rec = 5000
        val msgTraffic = calculateMessagesTrafficUseCase(rawItems)
        assertEquals(TrafficCategory.MESSAGES, msgTraffic.category)
        assertEquals(2000L, msgTraffic.sentBytes)
        assertEquals(5000L, msgTraffic.receivedBytes)
        assertEquals(42, msgTraffic.sentItems)
        assertEquals(84, msgTraffic.receivedItems)

        // Test boundary condition: media sum > total (should coerce to 0)
        rawItems[TrafficCategory.TOTAL] = TrafficItemModel(TrafficCategory.TOTAL, sentBytes = 5000L, receivedBytes = 5000L)
        val clampedTraffic = calculateMessagesTrafficUseCase(rawItems)
        assertEquals(0L, clampedTraffic.sentBytes)
        assertEquals(0L, clampedTraffic.receivedBytes)
    }

    @Test
    fun testFormatTrafficBytesAndCallsDuration() {
        assertEquals("0 B", formatTrafficBytesUseCase(0L))
        assertEquals("500 B", formatTrafficBytesUseCase(500L))
        assertEquals("1.0 KB", formatTrafficBytesUseCase(1024L))
        assertEquals("1.5 KB", formatTrafficBytesUseCase(1536L))
        assertEquals("1.0 MB", formatTrafficBytesUseCase(1024L * 1024L))
        assertEquals("2.5 MB", formatTrafficBytesUseCase((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.0 GB", formatTrafficBytesUseCase(1024L * 1024L * 1024L))

        assertEquals("0s", formatCallsDurationUseCase(0))
        assertEquals("45s", formatCallsDurationUseCase(45))
        assertEquals("2m 5s", formatCallsDurationUseCase(125))
        assertEquals("1h 1m 5s", formatCallsDurationUseCase(3665))
    }

    @Test
    fun testIncrementBytesItemsAndCallsDuration() = runTest {
        val net = NetworkType.WIFI

        // Increment bytes
        incrementTrafficBytesUseCase(
            networkType = net,
            category = TrafficCategory.VIDEOS,
            sentBytes = 1048576L,
            receivedBytes = 2097152L
        )

        // Increment items
        incrementTrafficItemsUseCase(
            networkType = net,
            category = TrafficCategory.VIDEOS,
            sentItems = 5,
            receivedItems = 10
        )

        // Increment calls duration
        incrementCallsTimeUseCase(networkType = net, seconds = 180)

        val summary = getStatsUseCase(net)
        val videoItem = summary.getItem(TrafficCategory.VIDEOS)

        assertEquals(1048576L, videoItem.sentBytes)
        assertEquals(2097152L, videoItem.receivedBytes)
        assertEquals(3145728L, videoItem.totalBytes)
        assertEquals(5, videoItem.sentItems)
        assertEquals(10, videoItem.receivedItems)
        assertEquals(15, videoItem.totalItems)
        assertEquals(180, summary.callsTotalTimeSec)
    }

    @Test
    fun testResetStats() = runTest {
        val mobile = NetworkType.MOBILE
        val wifi = NetworkType.WIFI

        // Add stats to mobile and wifi
        incrementTrafficBytesUseCase(mobile, TrafficCategory.PHOTOS, sentBytes = 5000L, receivedBytes = 10000L)
        incrementTrafficBytesUseCase(wifi, TrafficCategory.PHOTOS, sentBytes = 2000L, receivedBytes = 4000L)
        incrementCallsTimeUseCase(mobile, 60)
        incrementCallsTimeUseCase(wifi, 120)

        // Reset mobile
        resetStatsUseCase(mobile)

        val mobileSummary = getStatsUseCase(mobile)
        val wifiSummary = getStatsUseCase(wifi)

        // Mobile stats must be zeroed out
        assertEquals(0L, mobileSummary.getItem(TrafficCategory.PHOTOS).sentBytes)
        assertEquals(0L, mobileSummary.getItem(TrafficCategory.PHOTOS).receivedBytes)
        assertEquals(0, mobileSummary.callsTotalTimeSec)
        assertTrue(mobileSummary.resetStatsDateMs > 0L)

        // Wifi stats must remain intact
        assertEquals(2000L, wifiSummary.getItem(TrafficCategory.PHOTOS).sentBytes)
        assertEquals(4000L, wifiSummary.getItem(TrafficCategory.PHOTOS).receivedBytes)
        assertEquals(120, wifiSummary.callsTotalTimeSec)
    }

    @Test
    fun testNetworkStatsViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = NetworkStatsViewModel(
            observeAllNetworkStatsUseCase = observeAllStatsUseCase,
            getNetworkStatsUseCase = getStatsUseCase,
            resetNetworkStatsUseCase = resetStatsUseCase,
            refreshNetworkStatsUseCase = refreshStatsUseCase,
            incrementTrafficBytesUseCase = incrementTrafficBytesUseCase,
            incrementTrafficItemsUseCase = incrementTrafficItemsUseCase,
            incrementCallsTimeUseCase = incrementCallsTimeUseCase,
            formatTrafficBytesUseCase = formatTrafficBytesUseCase,
            formatCallsDurationUseCase = formatCallsDurationUseCase,
            scope = testScope
        )

        testScope.advanceUntilIdle()

        // Initial state check
        val initialUiState = viewModel.uiState.value
        assertEquals(NetworkType.MOBILE, initialUiState.selectedNetworkType)
        assertNotNull(initialUiState.currentSummary)

        // Select WiFi network
        viewModel.onEvent(NetworkStatsEvent.SelectNetworkType(NetworkType.WIFI))
        testScope.advanceUntilIdle()
        assertEquals(NetworkType.WIFI, viewModel.uiState.value.selectedNetworkType)

        // Increment WiFi traffic
        viewModel.onEvent(
            NetworkStatsEvent.IncrementTraffic(
                networkType = NetworkType.WIFI,
                category = TrafficCategory.AUDIOS,
                sentBytes = 1024L,
                receivedBytes = 2048L,
                sentItems = 2,
                receivedItems = 4
            )
        )
        viewModel.onEvent(NetworkStatsEvent.IncrementCallsTime(NetworkType.WIFI, 65))
        testScope.advanceUntilIdle()

        val wifiSummary = viewModel.uiState.value.currentSummary
        assertNotNull(wifiSummary)
        val audioItem = wifiSummary?.items?.get(TrafficCategory.AUDIOS)
        assertEquals("1.0 KB", audioItem?.sentBytesFormatted)
        assertEquals("2.0 KB", audioItem?.receivedBytesFormatted)
        assertEquals(2, audioItem?.sentItemsCount)
        assertEquals(4, audioItem?.receivedItemsCount)
        assertEquals("1m 5s", wifiSummary?.callsDurationFormatted)

        // Reset WiFi stats
        viewModel.onEvent(NetworkStatsEvent.ResetStats(NetworkType.WIFI))
        testScope.advanceUntilIdle()

        val resetSummary = viewModel.uiState.value.currentSummary
        val resetAudioItem = resetSummary?.items?.get(TrafficCategory.AUDIOS)
        assertEquals("0 B", resetAudioItem?.sentBytesFormatted)
        assertEquals("0s", resetSummary?.callsDurationFormatted)
        assertEquals("Statistics reset for WIFI", viewModel.uiState.value.infoMessage)

        viewModel.onEvent(NetworkStatsEvent.ClearMessage)
        assertEquals(null, viewModel.uiState.value.infoMessage)

        viewModel.onCleared()
    }
}
