package org.telegram.messenger.feature.social.location.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.data.datasource.LocationLocalDataSource
import org.telegram.messenger.feature.social.location.data.datasource.LocationRemoteDataSource
import org.telegram.messenger.feature.social.location.data.mapper.LocationMapper
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Clean domain repository implementation coordinating local SQLite / in-memory cache and
 * remote MTProto RPCs for location sharing and peer coordinates.
 */
class LocationRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: LocationLocalDataSource,
    private val remoteDataSource: LocationRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : LocationRepository {

    private val localSharingsMap = Collections.synchronizedMap(LinkedHashMap<Long, LiveLocationSharingModel>())
    private val peerLocationsMap = ConcurrentHashMap<Long, List<PeerLiveLocationModel>>()
    private val lastKnownLocationState = MutableStateFlow<GeoPointModel?>(null)

    init {
        setupNotificationListeners()
    }

    private fun setupNotificationListeners() {
        scope.launch {
            try {
                NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.newLocationAvailable)
                    .collect {
                        val loc = localDataSource.getLastKnownLocation()
                        lastKnownLocationState.value = LocationMapper.toDomain(loc)
                    }
            } catch (_: Throwable) {
                // Ignore under headless test environments
            }
        }
    }

    override fun observeActiveSharings(): Flow<List<LiveLocationSharingModel>> {
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.liveLocationsChanged)
            .map {
                getActiveSharings().getOrDefault(emptyList())
            }
            .onStart {
                emit(getActiveSharings().getOrDefault(emptyList()))
            }
    }

    override fun observePeerLocations(dialogId: Long): Flow<List<PeerLiveLocationModel>> {
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.liveLocationsCacheChanged)
            .filter { event ->
                if (event.args.isEmpty()) {
                    true
                } else {
                    val eventDialogId = (event.args[0] as? Number)?.toLong() ?: 0L
                    eventDialogId == 0L || eventDialogId == dialogId || eventDialogId == -dialogId
                }
            }
            .map {
                loadPeerLiveLocations(dialogId).getOrDefault(emptyList())
            }
            .onStart {
                emit(loadPeerLiveLocations(dialogId).getOrDefault(emptyList()))
            }
    }

    override fun observeLastKnownLocation(): Flow<GeoPointModel?> {
        return lastKnownLocationState.asStateFlow()
    }

    override suspend fun getActiveSharings(): Result<List<LiveLocationSharingModel>> = withContext(ioDispatcher) {
        try {
            val controllerSharings = localDataSource.getSharingLocationsUI()
            val mapped = LocationMapper.toDomainList(controllerSharings)
            if (mapped.isNotEmpty()) {
                synchronized(localSharingsMap) {
                    localSharingsMap.clear()
                    mapped.forEach { localSharingsMap[it.dialogId] = it }
                }
                Result.Success(mapped)
            } else {
                val fallbackList = synchronized(localSharingsMap) {
                    localSharingsMap.values.toList()
                }
                Result.Success(fallbackList)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get active sharings: ${e.message}", e))
        }
    }

    override suspend fun isSharingLocation(dialogId: Long): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val isSharing = localDataSource.isSharingLocation(dialogId) || localSharingsMap.containsKey(dialogId)
            Result.Success(isSharing)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check sharing for $dialogId: ${e.message}", e))
        }
    }

    override suspend fun getSharingInfo(dialogId: Long): Result<LiveLocationSharingModel?> = withContext(ioDispatcher) {
        try {
            val info = localDataSource.getSharingLocationInfo(dialogId)
            val domain = LocationMapper.toDomain(info) ?: localSharingsMap[dialogId]
            Result.Success(domain)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get sharing info for $dialogId: ${e.message}", e))
        }
    }

    override suspend fun getLastKnownLocation(): Result<GeoPointModel?> = withContext(ioDispatcher) {
        try {
            val location = localDataSource.getLastKnownLocation()
            val domain = LocationMapper.toDomain(location) ?: lastKnownLocationState.value
            Result.Success(domain)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get last known location: ${e.message}", e))
        }
    }

    override suspend fun loadPeerLiveLocations(dialogId: Long): Result<List<PeerLiveLocationModel>> = withContext(ioDispatcher) {
        try {
            localDataSource.loadLiveLocations(dialogId)
            val messages = localDataSource.getLocationsCache(dialogId)
            val domainList = LocationMapper.toPeerLocationList(dialogId, messages)
            if (domainList.isNotEmpty()) {
                peerLocationsMap[dialogId] = domainList
                Result.Success(domainList)
            } else {
                val cached = peerLocationsMap[dialogId] ?: emptyList()
                Result.Success(cached)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to load peer live locations for $dialogId: ${e.message}", e))
        }
    }

    override suspend fun stopLocationSharing(dialogId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            localSharingsMap.remove(dialogId)
            localDataSource.removeSharing(dialogId)
            localDataSource.removeSharingLocation(dialogId)
            postNotificationSafely(NotificationCenter.liveLocationsChanged)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to stop location sharing for $dialogId: ${e.message}", e))
        }
    }

    override suspend fun stopAllLocationSharings(): Result<Unit> = withContext(ioDispatcher) {
        try {
            localSharingsMap.clear()
            localDataSource.clearAllSharings()
            localDataSource.removeAllLocationSharings()
            postNotificationSafely(NotificationCenter.liveLocationsChanged)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to stop all location sharings: ${e.message}", e))
        }
    }

    override suspend fun setProximityAlert(dialogId: Long, distanceMeters: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            localDataSource.saveProximity(dialogId, distanceMeters)
            localDataSource.setProximityLocation(dialogId, distanceMeters, true)
            val existing = localSharingsMap[dialogId]
            if (existing != null) {
                localSharingsMap[dialogId] = existing.copy(proximityMeters = distanceMeters)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to set proximity alert for $dialogId: ${e.message}", e))
        }
    }

    override suspend fun sendStaticLocation(
        dialogId: Long,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.sendStaticLocation(dialogId, latitude, longitude)
    }

    override suspend fun sendLiveLocation(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int
    ): Result<Unit> = withContext(mainDispatcher) {
        val res = localDataSource.sendLiveLocation(dialogId, latitude, longitude, periodSeconds, proximityRadiusMeters)
        if (res is Result.Success) {
            val newSharing = LiveLocationSharingModel(
                dialogId = dialogId,
                messageId = 0,
                stopTime = (System.currentTimeMillis() / 1000L) + periodSeconds,
                period = periodSeconds,
                proximityMeters = proximityRadiusMeters,
                isActive = true
            )
            localSharingsMap[dialogId] = newSharing
            postNotificationSafely(NotificationCenter.liveLocationsChanged)
        }
        res
    }

    override suspend fun markLiveLocationsAsRead(dialogId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            localDataSource.markLiveLocationsAsRead(dialogId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to mark live locations as read for $dialogId: ${e.message}", e))
        }
    }

    /**
     * Helpers for testing and manual coordination.
     */
    fun updateLastKnownLocation(geoPoint: GeoPointModel?) {
        lastKnownLocationState.value = geoPoint
        postNotificationSafely(NotificationCenter.newLocationAvailable)
    }

    fun putPeerLocations(dialogId: Long, locations: List<PeerLiveLocationModel>) {
        peerLocationsMap[dialogId] = locations
        postNotificationSafely(NotificationCenter.liveLocationsCacheChanged, dialogId, currentAccount)
    }

    fun putActiveSharing(sharing: LiveLocationSharingModel) {
        localSharingsMap[sharing.dialogId] = sharing
        postNotificationSafely(NotificationCenter.liveLocationsChanged)
    }

    private fun postNotificationSafely(id: Int, vararg args: Any?) {
        try {
            NotificationCenter.getGlobalInstance().postNotificationName(id, *args)
        } catch (_: Throwable) {
            // Ignore when Android main looper / NotificationCenter is not mocked
        }
    }
}
