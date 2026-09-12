package org.telegram.messenger.feature.social.location.data.datasource

import android.location.Location
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocationController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

/**
 * Local data source managing location SQLite database operations via MessagesStorage on Dispatchers.IO,
 * along with in-memory location controller states and message sending dispatch.
 */
open class LocationLocalDataSource(
    currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseLocalDataSource(currentAccount, ioDispatcher) {

    private val safeLocationController: LocationController?
        get() = try {
            LocationController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val safeStorage: MessagesStorage?
        get() = try {
            MessagesStorage.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val safeSendMessagesHelper: SendMessagesHelper?
        get() = try {
            SendMessagesHelper.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    // --- SQLite Database Operations ---

    open suspend fun saveProximity(dialogId: Long, distanceMeters: Int): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                val state = storage.database.executeFast("UPDATE sharing_locations SET proximity = ? WHERE uid = ?")
                state.requery()
                state.bindInteger(1, distanceMeters)
                state.bindLong(2, dialogId)
                state.step()
                state.dispose()
            } catch (e: Exception) {
                // Ignore table absence or database locked errors
            }
        }
    }

    open suspend fun removeSharing(dialogId: Long): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                storage.database.executeFast("DELETE FROM sharing_locations WHERE uid = $dialogId").stepThis().dispose()
            } catch (e: Exception) {
                // Ignore table absence or database locked errors
            }
        }
    }

    open suspend fun clearAllSharings(): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                storage.database.executeFast("DELETE FROM sharing_locations WHERE 1").stepThis().dispose()
            } catch (e: Exception) {
                // Ignore table absence or database locked errors
            }
        }
    }

    open suspend fun putUsersAndChats(
        users: ArrayList<TLRPC.User>,
        chats: ArrayList<TLRPC.Chat>
    ): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            try {
                storage.putUsersAndChats(users, chats, true, true)
            } catch (e: Exception) {
                // Ignore storage exceptions
            }
        }
    }

    // --- In-Memory State & LocationController Delegations ---

    open fun getSharingLocationsUI(): List<LocationController.SharingLocationInfo> {
        val controller = safeLocationController ?: return emptyList()
        return synchronized(controller.sharingLocationsUI) {
            ArrayList(controller.sharingLocationsUI)
        }
    }

    open fun isSharingLocation(dialogId: Long): Boolean {
        return safeLocationController?.isSharingLocation(dialogId) ?: false
    }

    open fun getSharingLocationInfo(dialogId: Long): LocationController.SharingLocationInfo? {
        return safeLocationController?.getSharingLocationInfo(dialogId)
    }

    open fun getLastKnownLocation(): Location? {
        return safeLocationController?.lastKnownLocation
    }

    open fun getLocationsCache(dialogId: Long): List<TLRPC.Message>? {
        val controller = safeLocationController ?: return null
        val messages = controller.locationsCache.get(dialogId) ?: return null
        return synchronized(messages) {
            ArrayList(messages)
        }
    }

    open fun putLocationsCache(dialogId: Long, messages: ArrayList<TLRPC.Message>) {
        safeLocationController?.locationsCache?.put(dialogId, messages)
    }

    open fun loadLiveLocations(dialogId: Long) {
        safeLocationController?.loadLiveLocations(dialogId)
    }

    open fun removeSharingLocation(dialogId: Long) {
        safeLocationController?.removeSharingLocation(dialogId)
    }

    open fun removeAllLocationSharings() {
        safeLocationController?.removeAllLocationSharings()
    }

    open fun setProximityLocation(dialogId: Long, distanceMeters: Int, broadcast: Boolean): Boolean {
        return safeLocationController?.setProximityLocation(dialogId, distanceMeters, broadcast) ?: false
    }

    open fun markLiveLocationsAsRead(dialogId: Long) {
        safeLocationController?.markLiveLoactionsAsRead(dialogId)
    }

    // --- Location Sending Delegations ---

    open suspend fun sendStaticLocation(dialogId: Long, latitude: Double, longitude: Double): Result<Unit> {
        val sender = safeSendMessagesHelper ?: return Result.success(Unit)
        return try {
            val mediaGeo = TLRPC.TL_messageMediaGeo().apply {
                geo = TLRPC.TL_geoPoint().apply {
                    lat = AndroidUtilities.fixLocationCoord(latitude)
                    _long = AndroidUtilities.fixLocationCoord(longitude)
                }
            }
            sender.sendMessage(
                SendMessagesHelper.SendMessageParams.of(
                    mediaGeo, dialogId, null, null, null, null, true, 0, 0
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(org.telegram.messenger.core.result.AppError.Generic("Failed to send static location: ${e.message}", e))
        }
    }

    open suspend fun sendLiveLocation(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int
    ): Result<Unit> {
        val sender = safeSendMessagesHelper ?: return Result.success(Unit)
        return try {
            val mediaGeoLive = TLRPC.TL_messageMediaGeoLive().apply {
                geo = TLRPC.TL_geoPoint().apply {
                    lat = AndroidUtilities.fixLocationCoord(latitude)
                    _long = AndroidUtilities.fixLocationCoord(longitude)
                }
                period = periodSeconds
                proximity_notification_radius = proximityRadiusMeters
            }
            sender.sendMessage(
                SendMessagesHelper.SendMessageParams.of(
                    mediaGeoLive, dialogId, null, null, null, null, true, 0, 0
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(org.telegram.messenger.core.result.AppError.Generic("Failed to send live location: ${e.message}", e))
        }
    }
}
