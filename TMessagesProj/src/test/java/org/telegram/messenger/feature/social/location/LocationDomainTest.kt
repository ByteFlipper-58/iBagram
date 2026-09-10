package org.telegram.messenger.feature.social.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.data.mapper.LocationMapper
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository
import org.telegram.messenger.feature.social.location.domain.usecase.GetActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetSharingInfoUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.IsSharingLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.LoadPeerLiveLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.MarkLiveLocationsAsReadUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObservePeerLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendLiveLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendStaticLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SetProximityAlertUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopAllLocationSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopLocationSharingUseCase
import org.telegram.messenger.feature.social.location.presentation.LocationEvent
import org.telegram.messenger.feature.social.location.presentation.LocationViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class LocationDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGeoPointAndSharingModels() {
        val geo = GeoPointModel(
            latitude = 55.7558,
            longitude = 37.6173,
            accuracy = 5.0f
        )
        assertEquals(55.7558, geo.latitude, 0.0001)
        assertEquals(37.6173, geo.longitude, 0.0001)
        assertEquals(5.0f, geo.accuracy)

        val futureStopTime = (System.currentTimeMillis() / 1000L) + 3600
        val sharing = LiveLocationSharingModel(
            dialogId = 123456L,
            messageId = 10,
            stopTime = futureStopTime,
            period = 3600,
            proximityMeters = 50,
            isActive = true
        )
        assertEquals(123456L, sharing.dialogId)
        assertEquals(10, sharing.messageId)
        assertTrue(sharing.isActive)
        assertEquals(50, sharing.proximityMeters)

        val peerLocation = PeerLiveLocationModel(
            messageId = 100,
            dialogId = 123456L,
            fromId = 987654L,
            geoPoint = geo,
            date = 1700000000L,
            expiresIn = 1800,
            unread = false
        )
        assertEquals(100, peerLocation.messageId)
        assertEquals(987654L, peerLocation.fromId)
        assertEquals(55.7558, peerLocation.geoPoint.latitude, 0.0001)
        assertFalse(peerLocation.unread)
    }

    @Test
    fun testLocationMapper() {
        val futureStopTime = ((System.currentTimeMillis() / 1000L) + 1800).toInt()
        val sharingInfo = LocationController.SharingLocationInfo().apply {
            did = 777L
            mid = 50
            stopTime = futureStopTime
            period = 1800
            proximityMeters = 100
        }

        val domainSharing = LocationMapper.toDomain(sharingInfo)
        assertNotNull(domainSharing)
        domainSharing?.let {
            assertEquals(777L, it.dialogId)
            assertEquals(50, it.messageId)
            assertEquals(1800, it.period)
            assertEquals(100, it.proximityMeters)
            assertTrue(it.isActive)
        }

        val sharingList = LocationMapper.toDomainList(listOf(sharingInfo))
        assertEquals(1, sharingList.size)
        assertEquals(777L, sharingList[0].dialogId)

        // Mapping TLRPC.Message with TL_messageMediaGeoLive
        val msg = TLRPC.TL_message().apply {
            id = 200
            date = 1700001000
            unread = true
            from_id = TLRPC.TL_peerUser().apply { user_id = 9999L }
            media = TLRPC.TL_messageMediaGeoLive().apply {
                geo = TLRPC.TL_geoPoint().apply {
                    lat = 40.7128
                    _long = -74.0060
                }
                period = 900
            }
        }

        val peerDomain = LocationMapper.toPeerLocation(12345L, msg)
        assertNotNull(peerDomain)
        peerDomain?.let {
            assertEquals(200, it.messageId)
            assertEquals(12345L, it.dialogId)
            assertEquals(9999L, it.fromId)
            assertEquals(40.7128, it.geoPoint.latitude, 0.0001)
            assertEquals(-74.0060, it.geoPoint.longitude, 0.0001)
            assertEquals(900, it.expiresIn)
            assertTrue(it.unread)
        }
    }

    @Test
    fun testLocationUseCases() = runBlocking {
        val fakeRepo = FakeLocationRepository()
        val getActive = GetActiveSharingsUseCase(fakeRepo)
        val isSharing = IsSharingLocationUseCase(fakeRepo)
        val getInfo = GetSharingInfoUseCase(fakeRepo)
        val getLastKnown = GetLastKnownLocationUseCase(fakeRepo)
        val loadPeer = LoadPeerLiveLocationsUseCase(fakeRepo)
        val stopSharing = StopLocationSharingUseCase(fakeRepo)
        val stopAll = StopAllLocationSharingsUseCase(fakeRepo)
        val setProximity = SetProximityAlertUseCase(fakeRepo)
        val sendStatic = SendStaticLocationUseCase(fakeRepo)
        val sendLive = SendLiveLocationUseCase(fakeRepo)
        val markRead = MarkLiveLocationsAsReadUseCase(fakeRepo)

        // Active sharings
        val activeRes = getActive()
        assertTrue(activeRes.isSuccess)
        assertEquals(1, activeRes.getOrNull()?.size)

        // Is sharing
        val isSharingRes = isSharing(100L)
        assertTrue(isSharingRes.isSuccess)
        assertTrue(isSharingRes.getOrNull() == true)

        // Sharing info
        val infoRes = getInfo(100L)
        assertTrue(infoRes.isSuccess)
        assertEquals(100L, infoRes.getOrNull()?.dialogId)

        // Last known location
        val lastKnownRes = getLastKnown()
        assertTrue(lastKnownRes.isSuccess)
        assertNotNull(lastKnownRes.getOrNull())

        // Load peer locations
        val peersRes = loadPeer(100L)
        assertTrue(peersRes.isSuccess)
        assertEquals(2, peersRes.getOrNull()?.size)

        // Set proximity alert
        val proximityRes = setProximity(100L, 200)
        assertTrue(proximityRes.isSuccess)

        // Send static
        val sendStaticRes = sendStatic(100L, 10.0, 20.0)
        assertTrue(sendStaticRes.isSuccess)

        // Send live
        val sendLiveRes = sendLive(200L, 10.0, 20.0, 1800, 50)
        assertTrue(sendLiveRes.isSuccess)

        // Mark read
        val markReadRes = markRead(100L)
        assertTrue(markReadRes.isSuccess)

        // Stop sharing
        val stopRes = stopSharing(100L)
        assertTrue(stopRes.isSuccess)
        assertFalse(isSharing(100L).getOrNull() == true)

        // Stop all
        val stopAllRes = stopAll()
        assertTrue(stopAllRes.isSuccess)
        assertTrue(getActive().getOrNull()?.isEmpty() == true)
    }

    @Test
    fun testLocationViewModelAndEvents() = runBlocking {
        val fakeRepo = FakeLocationRepository()
        val viewModel = LocationViewModel(
            observeActiveSharingsUseCase = ObserveActiveSharingsUseCase(fakeRepo),
            observePeerLocationsUseCase = ObservePeerLocationsUseCase(fakeRepo),
            observeLastKnownLocationUseCase = ObserveLastKnownLocationUseCase(fakeRepo),
            getActiveSharingsUseCase = GetActiveSharingsUseCase(fakeRepo),
            isSharingLocationUseCase = IsSharingLocationUseCase(fakeRepo),
            getSharingInfoUseCase = GetSharingInfoUseCase(fakeRepo),
            getLastKnownLocationUseCase = GetLastKnownLocationUseCase(fakeRepo),
            loadPeerLiveLocationsUseCase = LoadPeerLiveLocationsUseCase(fakeRepo),
            stopLocationSharingUseCase = StopLocationSharingUseCase(fakeRepo),
            stopAllLocationSharingsUseCase = StopAllLocationSharingsUseCase(fakeRepo),
            setProximityAlertUseCase = SetProximityAlertUseCase(fakeRepo),
            sendStaticLocationUseCase = SendStaticLocationUseCase(fakeRepo),
            sendLiveLocationUseCase = SendLiveLocationUseCase(fakeRepo),
            markLiveLocationsAsReadUseCase = MarkLiveLocationsAsReadUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Initially observed active sharings from fake repo (dialog 100L is sharing)
        assertEquals(1, viewModel.uiState.value.myActiveSharings.size)
        assertNotNull(viewModel.uiState.value.lastKnownLocation)

        // Load dialog 100L
        viewModel.onEvent(LocationEvent.LoadDialog(100L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100L, viewModel.uiState.value.dialogId)
        assertTrue(viewModel.uiState.value.isCurrentDialogSharing)
        assertNotNull(viewModel.uiState.value.currentDialogSharing)
        assertEquals(2, viewModel.uiState.value.peerLocations.size)

        // Send static
        viewModel.onEvent(LocationEvent.SendStatic(100L, 55.0, 37.0))
        testDispatcher.scheduler.advanceUntilIdle()

        // Set proximity
        viewModel.onEvent(LocationEvent.SetProximity(100L, 150))
        testDispatcher.scheduler.advanceUntilIdle()

        // Stop sharing
        viewModel.onEvent(LocationEvent.StopSharing(100L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCurrentDialogSharing)
        assertNull(viewModel.uiState.value.currentDialogSharing)
    }

    private class FakeLocationRepository : LocationRepository {
        val activeSharings = mutableListOf(
            LiveLocationSharingModel(dialogId = 100L, messageId = 1, stopTime = 9999999999L, period = 3600, proximityMeters = 0, isActive = true)
        )

        val peerLocationsMap = mutableMapOf<Long, MutableList<PeerLiveLocationModel>>(
            100L to mutableListOf(
                PeerLiveLocationModel(messageId = 10, dialogId = 100L, fromId = 555L, geoPoint = GeoPointModel(55.0, 37.0), date = 1000L),
                PeerLiveLocationModel(messageId = 11, dialogId = 100L, fromId = 666L, geoPoint = GeoPointModel(55.1, 37.1), date = 1010L)
            )
        )

        val lastLocation = GeoPointModel(55.75, 37.61, 10.0f)

        private val activeSharingsFlow = MutableStateFlow<List<LiveLocationSharingModel>>(activeSharings.toList())
        private val lastLocationFlow = MutableStateFlow<GeoPointModel?>(lastLocation)

        override fun observeActiveSharings(): Flow<List<LiveLocationSharingModel>> = activeSharingsFlow.asStateFlow()

        override fun observePeerLocations(dialogId: Long): Flow<List<PeerLiveLocationModel>> =
            MutableStateFlow(peerLocationsMap[dialogId] ?: emptyList()).asStateFlow()

        override fun observeLastKnownLocation(): Flow<GeoPointModel?> = lastLocationFlow.asStateFlow()

        override suspend fun getActiveSharings(): Result<List<LiveLocationSharingModel>> =
            Result.Success(activeSharings.toList())

        override suspend fun isSharingLocation(dialogId: Long): Result<Boolean> =
            Result.Success(activeSharings.any { it.dialogId == dialogId })

        override suspend fun getSharingInfo(dialogId: Long): Result<LiveLocationSharingModel?> =
            Result.Success(activeSharings.find { it.dialogId == dialogId })

        override suspend fun getLastKnownLocation(): Result<GeoPointModel?> =
            Result.Success(lastLocation)

        override suspend fun loadPeerLiveLocations(dialogId: Long): Result<List<PeerLiveLocationModel>> =
            Result.Success(peerLocationsMap[dialogId] ?: emptyList())

        override suspend fun stopLocationSharing(dialogId: Long): Result<Unit> {
            activeSharings.removeAll { it.dialogId == dialogId }
            activeSharingsFlow.value = activeSharings.toList()
            return Result.Success(Unit)
        }

        override suspend fun stopAllLocationSharings(): Result<Unit> {
            activeSharings.clear()
            activeSharingsFlow.value = emptyList()
            return Result.Success(Unit)
        }

        override suspend fun setProximityAlert(dialogId: Long, distanceMeters: Int): Result<Unit> {
            val index = activeSharings.indexOfFirst { it.dialogId == dialogId }
            if (index != -1) {
                activeSharings[index] = activeSharings[index].copy(proximityMeters = distanceMeters)
                activeSharingsFlow.value = activeSharings.toList()
            }
            return Result.Success(Unit)
        }

        override suspend fun sendStaticLocation(dialogId: Long, latitude: Double, longitude: Double): Result<Unit> =
            Result.Success(Unit)

        override suspend fun sendLiveLocation(
            dialogId: Long,
            latitude: Double,
            longitude: Double,
            periodSeconds: Int,
            proximityRadiusMeters: Int
        ): Result<Unit> {
            activeSharings.add(
                LiveLocationSharingModel(
                    dialogId = dialogId,
                    messageId = 99,
                    stopTime = System.currentTimeMillis() / 1000L + periodSeconds,
                    period = periodSeconds,
                    proximityMeters = proximityRadiusMeters,
                    isActive = true
                )
            )
            activeSharingsFlow.value = activeSharings.toList()
            return Result.Success(Unit)
        }

        override suspend fun markLiveLocationsAsRead(dialogId: Long): Result<Unit> =
            Result.Success(Unit)
    }
}
