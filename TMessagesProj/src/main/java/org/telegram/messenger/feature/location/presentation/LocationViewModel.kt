package org.telegram.messenger.feature.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.location.domain.usecase.GetActiveSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.GetLastKnownLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.GetSharingInfoUseCase
import org.telegram.messenger.feature.location.domain.usecase.IsSharingLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.LoadPeerLiveLocationsUseCase
import org.telegram.messenger.feature.location.domain.usecase.MarkLiveLocationsAsReadUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObserveActiveSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObserveLastKnownLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObservePeerLocationsUseCase
import org.telegram.messenger.feature.location.domain.usecase.SendLiveLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.SendStaticLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.SetProximityAlertUseCase
import org.telegram.messenger.feature.location.domain.usecase.StopAllLocationSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.StopLocationSharingUseCase

class LocationViewModel(
    private val observeActiveSharingsUseCase: ObserveActiveSharingsUseCase,
    private val observePeerLocationsUseCase: ObservePeerLocationsUseCase,
    private val observeLastKnownLocationUseCase: ObserveLastKnownLocationUseCase,
    private val getActiveSharingsUseCase: GetActiveSharingsUseCase,
    private val isSharingLocationUseCase: IsSharingLocationUseCase,
    private val getSharingInfoUseCase: GetSharingInfoUseCase,
    private val getLastKnownLocationUseCase: GetLastKnownLocationUseCase,
    private val loadPeerLiveLocationsUseCase: LoadPeerLiveLocationsUseCase,
    private val stopLocationSharingUseCase: StopLocationSharingUseCase,
    private val stopAllLocationSharingsUseCase: StopAllLocationSharingsUseCase,
    private val setProximityAlertUseCase: SetProximityAlertUseCase,
    private val sendStaticLocationUseCase: SendStaticLocationUseCase,
    private val sendLiveLocationUseCase: SendLiveLocationUseCase,
    private val markLiveLocationsAsReadUseCase: MarkLiveLocationsAsReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var peerLocationsJob: Job? = null

    init {
        observeActiveSharingsUseCase()
            .onEach { sharings ->
                _uiState.update { current ->
                    val isCurrent = current.dialogId != 0L && sharings.any { it.dialogId == current.dialogId }
                    val currentInfo = sharings.find { it.dialogId == current.dialogId }
                    current.copy(
                        myActiveSharings = sharings,
                        isCurrentDialogSharing = isCurrent,
                        currentDialogSharing = currentInfo
                    )
                }
            }
            .launchIn(viewModelScope)

        observeLastKnownLocationUseCase()
            .onEach { geo ->
                _uiState.update { it.copy(lastKnownLocation = geo) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: LocationEvent) {
        when (event) {
            is LocationEvent.LoadDialog -> loadDialog(event.dialogId)
            is LocationEvent.SendStatic -> sendStatic(event.dialogId, event.latitude, event.longitude)
            is LocationEvent.StartLiveSharing -> startLiveSharing(
                event.dialogId,
                event.latitude,
                event.longitude,
                event.periodSeconds,
                event.proximityRadiusMeters
            )
            is LocationEvent.StopSharing -> stopSharing(event.dialogId)
            is LocationEvent.StopAllSharing -> stopAllSharing()
            is LocationEvent.SetProximity -> setProximity(event.dialogId, event.distanceMeters)
            is LocationEvent.MarkAsRead -> markAsRead(event.dialogId)
            is LocationEvent.Refresh -> refresh()
        }
    }

    private fun loadDialog(dialogId: Long) {
        _uiState.update { current ->
            val isCurrent = current.myActiveSharings.any { it.dialogId == dialogId }
            val currentInfo = current.myActiveSharings.find { it.dialogId == dialogId }
            current.copy(
                dialogId = dialogId,
                isCurrentDialogSharing = isCurrent,
                currentDialogSharing = currentInfo,
                isLoading = true,
                errorMessage = null
            )
        }

        peerLocationsJob?.cancel()
        peerLocationsJob = observePeerLocationsUseCase(dialogId)
            .onEach { peers ->
                _uiState.update { it.copy(peerLocations = peers, isLoading = false) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            loadPeerLiveLocationsUseCase(dialogId)
        }
    }

    private fun sendStatic(dialogId: Long, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            sendStaticLocationUseCase(dialogId, latitude, longitude)
        }
    }

    private fun startLiveSharing(
        dialogId: Long,
        latitude: Double,
        longitude: Double,
        periodSeconds: Int,
        proximityRadiusMeters: Int
    ) {
        viewModelScope.launch {
            sendLiveLocationUseCase(dialogId, latitude, longitude, periodSeconds, proximityRadiusMeters)
        }
    }

    private fun stopSharing(dialogId: Long) {
        viewModelScope.launch {
            stopLocationSharingUseCase(dialogId)
        }
    }

    private fun stopAllSharing() {
        viewModelScope.launch {
            stopAllLocationSharingsUseCase()
        }
    }

    private fun setProximity(dialogId: Long, distanceMeters: Int) {
        viewModelScope.launch {
            setProximityAlertUseCase(dialogId, distanceMeters)
        }
    }

    private fun markAsRead(dialogId: Long) {
        viewModelScope.launch {
            markLiveLocationsAsReadUseCase(dialogId)
        }
    }

    private fun refresh() {
        val dialogId = _uiState.value.dialogId
        if (dialogId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadPeerLiveLocationsUseCase(dialogId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
