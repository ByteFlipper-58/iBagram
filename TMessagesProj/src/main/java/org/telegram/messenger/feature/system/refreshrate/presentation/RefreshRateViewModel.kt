package org.telegram.messenger.feature.system.refreshrate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.GetRefreshRateStateUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ObserveRefreshRateStateUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.RecordFrameMetricUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ResetRefreshRateStatsUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.SetPreferredRefreshRateModeUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.StartRefreshRateTrackingUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.StopRefreshRateTrackingUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ToggleAdaptiveRefreshRateUseCase

class RefreshRateViewModel(
    private val observeRefreshRateStateUseCase: ObserveRefreshRateStateUseCase,
    private val getRefreshRateStateUseCase: GetRefreshRateStateUseCase,
    private val startRefreshRateTrackingUseCase: StartRefreshRateTrackingUseCase,
    private val stopRefreshRateTrackingUseCase: StopRefreshRateTrackingUseCase,
    private val toggleAdaptiveRefreshRateUseCase: ToggleAdaptiveRefreshRateUseCase,
    private val setPreferredRefreshRateModeUseCase: SetPreferredRefreshRateModeUseCase,
    private val recordFrameMetricUseCase: RecordFrameMetricUseCase,
    private val resetRefreshRateStatsUseCase: ResetRefreshRateStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RefreshRateUiState(state = getRefreshRateStateUseCase()))
    val uiState: StateFlow<RefreshRateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeRefreshRateStateUseCase().collect { state ->
                _uiState.value = _uiState.value.copy(state = state)
            }
        }
    }

    fun onEvent(event: RefreshRateEvent) {
        when (event) {
            is RefreshRateEvent.StartTracking -> startTracking()
            is RefreshRateEvent.StopTracking -> stopTracking()
            is RefreshRateEvent.ToggleAdaptive -> toggleAdaptive(event.enabled)
            is RefreshRateEvent.SelectMode -> selectMode(event.mode)
            is RefreshRateEvent.RecordFrame -> recordFrame(event.durationNs)
            is RefreshRateEvent.ResetStats -> resetStats()
            is RefreshRateEvent.DismissInfo -> dismissInfo()
        }
    }

    private fun startTracking() {
        startRefreshRateTrackingUseCase()
    }

    private fun stopTracking() {
        stopRefreshRateTrackingUseCase()
    }

    private fun toggleAdaptive(enabled: Boolean) {
        toggleAdaptiveRefreshRateUseCase(enabled)
    }

    private fun selectMode(mode: DisplayRefreshModeModel) {
        setPreferredRefreshRateModeUseCase(mode)
    }

    private fun recordFrame(durationNs: Long) {
        recordFrameMetricUseCase(durationNs)
    }

    private fun resetStats() {
        resetRefreshRateStatsUseCase()
    }

    private fun dismissInfo() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
