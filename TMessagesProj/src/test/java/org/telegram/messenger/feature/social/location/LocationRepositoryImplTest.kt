package org.telegram.messenger.feature.social.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.LocationController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.data.datasource.LocationLocalDataSource
import org.telegram.messenger.feature.social.location.data.datasource.LocationRemoteDataSource
import org.telegram.messenger.feature.social.location.data.repository.LocationRepositoryImpl
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    private class FakeLocationLocalDataSource(account: Int) : LocationLocalDataSource(account) {
        var lastSavedProximityDialogId = 0L
        var lastSavedProximityMeters = 0
        var lastRemovedSharingDialogId = 0L
        var clearAllSharingsCalled = false
        var sendStaticCalled = false
        var sendLiveCalled = false
        var markReadCalled = false

        override suspend fun saveProximity(dialogId: Long, distanceMeters: Int): Result<Unit> {
            lastSavedProximityDialogId = dialogId
            lastSavedProximityMeters = distanceMeters
            return Result.success(Unit)
        }

        override suspend fun removeSharing(dialogId: Long): Result<Unit> {
            lastRemovedSharingDialogId = dialogId
            return Result.success(Unit)
        }

        override suspend fun clearAllSharings(): Result<Unit> {
            clearAllSharingsCalled = true
            return Result.success(Unit)
        }

        override suspend fun sendStaticLocation(dialogId: Long, latitude: Double, longitude: Double): Result<Unit> {
            sendStaticCalled = true
            return Result.success(Unit)
        }

        override suspend fun sendLiveLocation(
            dialogId: Long,
            latitude: Double,
            longitude: Double,
            periodSeconds: Int,
            proximityRadiusMeters: Int
        ): Result<Unit> {
            sendLiveCalled = true
            return Result.success(Unit)
        }

        override fun markLiveLocationsAsRead(dialogId: Long) {
            markReadCalled = true
        }

        override fun removeSharingLocation(dialogId: Long) {}
        override fun removeAllLocationSharings() {}
        override fun loadLiveLocations(dialogId: Long) {}
    }

    private class FakeLocationRemoteDataSource(account: Int) : LocationRemoteDataSource(account) {
        var stopLiveLocationCalled = false

        override suspend fun stopLiveLocation(peer: TLRPC.InputPeer, messageId: Int): Result<TLRPC.Updates> {
            stopLiveLocationCalled = true
            return Result.success(TLRPC.TL_updates())
        }
    }

    private fun createRepository(
        account: Int = 0,
        localDataSource: LocationLocalDataSource = FakeLocationLocalDataSource(account),
        remoteDataSource: LocationRemoteDataSource = FakeLocationRemoteDataSource(account),
        scope: CoroutineScope = CoroutineScope(testDispatcher)
    ): LocationRepositoryImpl {
        return LocationRepositoryImpl(
            currentAccount = account,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            scope = scope
        )
    }

    @Test
    fun getActiveSharings_returns_empty_by_default() = runTest {
        val repo = createRepository()
        val result = repo.getActiveSharings()
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun isSharingLocation_returns_true_when_sharing_present() = runTest {
        val repo = createRepository()
        assertFalse(repo.isSharingLocation(123L).getOrDefault(false))

        val sharing = LiveLocationSharingModel(
            dialogId = 123L,
            messageId = 1,
            stopTime = (System.currentTimeMillis() / 1000L) + 3600,
            period = 3600,
            proximityMeters = 100,
            isActive = true
        )
        repo.putActiveSharing(sharing)

        assertTrue(repo.isSharingLocation(123L).getOrDefault(false))
        val info = repo.getSharingInfo(123L).getOrNull()
        assertNotNull(info)
        assertEquals(100, info?.proximityMeters)
    }

    @Test
    fun stopLocationSharing_removes_from_repository_and_delegates_to_datasource() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val sharing = LiveLocationSharingModel(
            dialogId = 456L,
            messageId = 2,
            stopTime = 9999999999L,
            period = 1800,
            isActive = true
        )
        repo.putActiveSharing(sharing)
        assertTrue(repo.isSharingLocation(456L).getOrDefault(false))

        val stopRes = repo.stopLocationSharing(456L)
        assertTrue(stopRes is Result.Success)
        assertFalse(repo.isSharingLocation(456L).getOrDefault(true))
        assertEquals(456L, local.lastRemovedSharingDialogId)
    }

    @Test
    fun stopAllLocationSharings_clears_all_sharings() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        repo.putActiveSharing(LiveLocationSharingModel(1L, 10, 9999999L, 100, 0, true))
        repo.putActiveSharing(LiveLocationSharingModel(2L, 20, 9999999L, 100, 0, true))

        assertEquals(2, repo.getActiveSharings().getOrDefault(emptyList()).size)

        repo.stopAllLocationSharings()
        assertEquals(0, repo.getActiveSharings().getOrDefault(emptyList()).size)
        assertTrue(local.clearAllSharingsCalled)
    }

    @Test
    fun setProximityAlert_updates_alert_distance() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        repo.putActiveSharing(LiveLocationSharingModel(789L, 30, 9999999L, 300, 0, true))
        val res = repo.setProximityAlert(789L, 250)
        assertTrue(res is Result.Success)

        assertEquals(789L, local.lastSavedProximityDialogId)
        assertEquals(250, local.lastSavedProximityMeters)
        assertEquals(250, repo.getSharingInfo(789L).getOrNull()?.proximityMeters)
    }

    @Test
    fun sendStaticLocation_delegates_to_local_source() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val res = repo.sendStaticLocation(1001L, 40.7128, -74.0060)
        assertTrue(res is Result.Success)
        assertTrue(local.sendStaticCalled)
    }

    @Test
    fun sendLiveLocation_delegates_and_registers_sharing() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val res = repo.sendLiveLocation(2002L, 51.5074, -0.1278, 3600, 50)
        assertTrue(res is Result.Success)
        assertTrue(local.sendLiveCalled)
        assertTrue(repo.isSharingLocation(2002L).getOrDefault(false))
    }

    @Test
    fun loadPeerLiveLocations_and_observePeerLocations_flow() = runTest {
        val repo = createRepository()
        val peerLocation = PeerLiveLocationModel(
            messageId = 1,
            dialogId = 3003L,
            fromId = 555L,
            geoPoint = GeoPointModel(10.0, 20.0, 1.0f),
            date = 1000L,
            expiresIn = 600,
            unread = false
        )

        repo.putPeerLocations(3003L, listOf(peerLocation))
        val loaded = repo.loadPeerLiveLocations(3003L).getOrDefault(emptyList())
        assertEquals(1, loaded.size)
        assertEquals(1, loaded[0].messageId)
        assertEquals(555L, loaded[0].fromId)

        val observed = repo.observePeerLocations(3003L).first()
        assertEquals(1, observed.size)
    }

    @Test
    fun observeLastKnownLocation_updates_on_new_location() = runTest {
        val repo = createRepository()
        assertNull(repo.observeLastKnownLocation().first())

        val geo = GeoPointModel(37.7749, -122.4194, 3.0f)
        repo.updateLastKnownLocation(geo)

        val updated = repo.observeLastKnownLocation().first()
        assertNotNull(updated)
        assertEquals(37.7749, updated?.latitude ?: 0.0, 0.0001)
        assertEquals(-122.4194, updated?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun markLiveLocationsAsRead_delegates_to_local_source() = runTest {
        val local = FakeLocationLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val res = repo.markLiveLocationsAsRead(4004L)
        assertTrue(res is Result.Success)
        assertTrue(local.markReadCalled)
    }
}
