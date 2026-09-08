package org.telegram.messenger.feature.location.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.location.domain.model.PeerLiveLocationModel

/**
 * Domain repository contract for location, GPS, live sharing and proximity alerts.
 */
interface LocationRepository {

    /**
     * Observes all active live location sessions currently being shared by this account.
     */
    fun observeActiveSharings(): Flow<List<LiveLocationSharingModel>>

    /**
     * Observes incoming live locations from peers in a given dialog.
     */
    fun observePeerLocations(dialogId: Long): Flow<List<PeerLiveLocationModel>>

    /**
     * Observes the user's latest acquired GPS coordinate.
     */
    fun observeLastKnownLocation(): Flow<GeoPointModel?>

    /**
     * Returns current list of active location sharings.
     */
    suspend fun getActiveSharings(): Result<List<LiveLocationSharingModel>>

    /**
     * Checks if location is currently being shared in [dialogId].
     */
    suspend fun isSharingLocation(dialogId: Long): Result<Boolean>

    /**
     * Returns sharing details for [dialogId], or null if not sharing.
     */
    suspend fun getSharingInfo(dialogId: Long): Result<LiveLocationSharingModel?>

    /**
     * Returns the latest known location.
     */
    suspend fun getLastKnownLocation(): Result<GeoPointModel?>

    /**
     * Loads live locations for [dialogId] from cache/network.
     */
    suspend fun loadPeerLiveLocations(dialogId: Long): Result<List<PeerLiveLocationModel>>

    /**
     * Stops live location sharing in [dialogId].
     */
    suspend fun stopLocationSharing(dialogId: Long): Result<Unit>

    /**
     * Stops all active live location sharings across all dialogs.
     */
    suspend fun stopAllLocationSharings(): Result<Unit>

    /**
     * Sets a proximity alert for a peer's location in [dialogId].
     */
    suspend fun setProximityAlert(dialogId: Long, distanceMeters: Int): Result<Unit>

    /**
     * Sends a static location message to [dialogId].
     */
    suspend fun sendStaticLocation(dialogId: Long, latitude: Double, longitude: Double): Result<Unit>

    /**
     * Starts sharing a live location in [dialogId] for [periodSeconds].
     */
    suspend fun sendLiveLocation(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int = 0
    ): Result<Unit>

    /**
     * Marks peer live locations in [dialogId] as read.
     */
    suspend fun markLiveLocationsAsRead(dialogId: Long): Result<Unit>
}
