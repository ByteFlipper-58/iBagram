package org.telegram.messenger.feature.social.location.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocationController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.location.data.mapper.LocationMapper
import org.telegram.messenger.feature.social.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.social.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.social.location.domain.model.PeerLiveLocationModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository
import org.telegram.tgnet.TLRPC

/**
 * Adapter implementing [LocationRepository] on top of legacy [LocationController] and [SendMessagesHelper].
 * All controller and SendMessagesHelper interactions are dispatched safely on [Dispatchers.Main].
 */
class LegacyLocationRepository(
    private val account: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationRepository {

    private val locationController: LocationController
        get() = LocationController.getInstance(account)

    private val sendMessagesHelper: SendMessagesHelper
        get() = SendMessagesHelper.getInstance(account)

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
        return NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.newLocationAvailable)
            .map {
                getLastKnownLocation().getOrNull()
            }
            .onStart {
                emit(getLastKnownLocation().getOrNull())
            }
    }

    override suspend fun getActiveSharings(): Result<List<LiveLocationSharingModel>> = withContext(mainDispatcher) {
        try {
            val sharings = locationController.sharingLocationsUI
            Result.Success(LocationMapper.toDomainList(sharings))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get active sharings: ${e.message}", e))
        }
    }

    override suspend fun isSharingLocation(dialogId: Long): Result<Boolean> = withContext(mainDispatcher) {
        try {
            Result.Success(locationController.isSharingLocation(dialogId))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check if sharing location in dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun getSharingInfo(dialogId: Long): Result<LiveLocationSharingModel?> = withContext(mainDispatcher) {
        try {
            val info = locationController.getSharingLocationInfo(dialogId)
            Result.Success(LocationMapper.toDomain(info))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get sharing info for dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun getLastKnownLocation(): Result<GeoPointModel?> = withContext(mainDispatcher) {
        try {
            val location = locationController.lastKnownLocation
            Result.Success(LocationMapper.toDomain(location))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get last known location: ${e.message}", e))
        }
    }

    override suspend fun loadPeerLiveLocations(dialogId: Long): Result<List<PeerLiveLocationModel>> = withContext(mainDispatcher) {
        try {
            locationController.loadLiveLocations(dialogId)
            val messages = locationController.locationsCache.get(dialogId)
            Result.Success(LocationMapper.toPeerLocationList(dialogId, messages))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to load peer live locations for dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun stopLocationSharing(dialogId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            locationController.removeSharingLocation(dialogId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to stop location sharing for dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun stopAllLocationSharings(): Result<Unit> = withContext(mainDispatcher) {
        try {
            locationController.removeAllLocationSharings()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to stop all location sharings: ${e.message}", e))
        }
    }

    override suspend fun setProximityAlert(dialogId: Long, distanceMeters: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            locationController.setProximityLocation(dialogId, distanceMeters, true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to set proximity alert for dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun sendStaticLocation(dialogId: Long, latitude: Double, longitude: Double): Result<Unit> = withContext(mainDispatcher) {
        try {
            val mediaGeo = TLRPC.TL_messageMediaGeo().apply {
                geo = TLRPC.TL_geoPoint().apply {
                    lat = AndroidUtilities.fixLocationCoord(latitude)
                    _long = AndroidUtilities.fixLocationCoord(longitude)
                }
            }
            sendMessagesHelper.sendMessage(
                SendMessagesHelper.SendMessageParams.of(
                    mediaGeo, dialogId, null, null, null, null, true, 0, 0
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to send static location to dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun sendLiveLocation(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int
    ): Result<Unit> = withContext(mainDispatcher) {
        try {
            val mediaGeoLive = TLRPC.TL_messageMediaGeoLive().apply {
                geo = TLRPC.TL_geoPoint().apply {
                    lat = AndroidUtilities.fixLocationCoord(latitude)
                    _long = AndroidUtilities.fixLocationCoord(longitude)
                }
                period = periodSeconds
                proximity_notification_radius = proximityRadiusMeters
            }
            sendMessagesHelper.sendMessage(
                SendMessagesHelper.SendMessageParams.of(
                    mediaGeoLive, dialogId, null, null, null, null, true, 0, 0
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to send live location to dialog $dialogId: ${e.message}", e))
        }
    }

    override suspend fun markLiveLocationsAsRead(dialogId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            locationController.markLiveLoactionsAsRead(dialogId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to mark live locations as read for dialog $dialogId: ${e.message}", e))
        }
    }
}
