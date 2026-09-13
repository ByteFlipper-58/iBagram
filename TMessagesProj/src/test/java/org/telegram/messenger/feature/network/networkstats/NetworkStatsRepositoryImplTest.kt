package org.telegram.messenger.feature.network.networkstats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.network.networkstats.data.datasource.NetworkStatsLocalDataSource
import org.telegram.messenger.feature.network.networkstats.data.datasource.NetworkStatsRemoteDataSource
import org.telegram.messenger.feature.network.networkstats.data.repository.NetworkStatsRepositoryImpl
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficCategory

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: NetworkStatsLocalDataSource
    private lateinit var remoteDataSource: NetworkStatsRemoteDataSource
    private lateinit var repository: NetworkStatsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = object : NetworkStatsLocalDataSource(0) {
            override fun getStatsController() = null
        }
        remoteDataSource = NetworkStatsRemoteDataSource(0)
        repository = NetworkStatsRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetStatsInitial() = runTest {
        val summary = repository.getStats(NetworkType.MOBILE)
        assertNotNull(summary)
        assertEquals(NetworkType.MOBILE, summary.networkType)
        assertEquals(0L, summary.items[TrafficCategory.CALLS]?.sentBytes)
        assertEquals(0L, summary.items[TrafficCategory.TOTAL]?.sentBytes)
    }

    @Test
    fun testIncrementSentBytes() = runTest {
        repository.incrementSentBytes(NetworkType.WIFI, TrafficCategory.PHOTOS, 1024L)
        val summary = repository.getStats(NetworkType.WIFI)
        assertEquals(1024L, summary.items[TrafficCategory.PHOTOS]?.sentBytes)
        assertEquals(1024L, summary.items[TrafficCategory.TOTAL]?.sentBytes)
    }

    @Test
    fun testIncrementReceivedBytes() = runTest {
        repository.incrementReceivedBytes(NetworkType.ROAMING, TrafficCategory.VIDEOS, 2048L)
        val summary = repository.getStats(NetworkType.ROAMING)
        assertEquals(2048L, summary.items[TrafficCategory.VIDEOS]?.receivedBytes)
        assertEquals(2048L, summary.items[TrafficCategory.TOTAL]?.receivedBytes)
    }

    @Test
    fun testIncrementItemsCount() = runTest {
        repository.incrementSentItems(NetworkType.MOBILE, TrafficCategory.AUDIOS, 5)
        repository.incrementReceivedItems(NetworkType.MOBILE, TrafficCategory.AUDIOS, 3)
        val summary = repository.getStats(NetworkType.MOBILE)
        assertEquals(5, summary.items[TrafficCategory.AUDIOS]?.sentItems)
        assertEquals(3, summary.items[TrafficCategory.AUDIOS]?.receivedItems)
    }

    @Test
    fun testIncrementCallsTotalTime() = runTest {
        repository.incrementCallsTotalTime(NetworkType.WIFI, 120)
        val summary = repository.getStats(NetworkType.WIFI)
        assertEquals(120, summary.callsTotalTimeSec)
    }

    @Test
    fun testResetStats() = runTest {
        repository.incrementSentBytes(NetworkType.MOBILE, TrafficCategory.FILES, 5000L)
        repository.incrementCallsTotalTime(NetworkType.MOBILE, 60)
        assertEquals(5000L, repository.getStats(NetworkType.MOBILE).items[TrafficCategory.FILES]?.sentBytes)

        repository.resetStats(NetworkType.MOBILE)
        val afterReset = repository.getStats(NetworkType.MOBILE)
        assertEquals(0L, afterReset.items[TrafficCategory.FILES]?.sentBytes)
        assertEquals(0, afterReset.callsTotalTimeSec)
    }

    @Test
    fun testObserveStats() = runTest {
        repository.incrementSentBytes(NetworkType.WIFI, TrafficCategory.MUSIC, 4096L)
        val emitted = repository.observeStats(NetworkType.WIFI).first()
        assertEquals(4096L, emitted.items[TrafficCategory.MUSIC]?.sentBytes)
    }

    @Test
    fun testRefreshStats() = runTest {
        repository.refreshStats()
        val all = repository.getAllStats()
        assertEquals(3, all.size)
        assertTrue(all.containsKey(NetworkType.MOBILE))
        assertTrue(all.containsKey(NetworkType.WIFI))
        assertTrue(all.containsKey(NetworkType.ROAMING))
    }
}
