package org.telegram.messenger.feature.location.presentation

import org.telegram.messenger.feature.location.domain.model.GeoPointModel
import org.telegram.messenger.feature.location.domain.model.LiveLocationSharingModel
import org.telegram.messenger.feature.location.domain.model.PeerLiveLocationModel

/**
 * Immutable UI State for the location screen.
 */
data class LocationUiState(
    val dialogId: Long = 0L,
    val myActiveSharings: List<LiveLocationSharingModel> = emptyList(),
    val isCurrentDialogSharing: Boolean = false,
    val currentDialogSharing: LiveLocationSharingModel? = null,
    val peerLocations: List<PeerLiveLocationModel> = emptyList(),
    val lastKnownLocation: GeoPointModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
