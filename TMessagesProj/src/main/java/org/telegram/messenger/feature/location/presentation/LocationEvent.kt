package org.telegram.messenger.feature.location.presentation

/**
 * MVI Events for location features.
 */
sealed class LocationEvent {
    data class LoadDialog(val dialogId: Long) : LocationEvent()
    data class SendStatic(val dialogId: Long, val latitude: Double, val longitude: Double) : LocationEvent()
    data class StartLiveSharing(
        val dialogId: Long,
        val latitude: Double,
        val longitude: Double,
        val periodSeconds: Int,
        val proximityRadiusMeters: Int = 0
    ) : LocationEvent()
    data class StopSharing(val dialogId: Long) : LocationEvent()
    object StopAllSharing : LocationEvent()
    data class SetProximity(val dialogId: Long, val distanceMeters: Int) : LocationEvent()
    data class MarkAsRead(val dialogId: Long) : LocationEvent()
    object Refresh : LocationEvent()
}
