package org.telegram.messenger.feature.system.refreshrate.presentation

import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel

/**
 * UI State for display refresh rate management.
 */
data class RefreshRateUiState(
    val state: RefreshRateStateModel = RefreshRateStateModel(),
    val isLoading: Boolean = false,
    val infoMessage: String? = null
) {
    val isTracking: Boolean get() = state.isTrackingActive
    val fps: Float get() = state.currentFps
    val isAdaptive: Boolean get() = state.isAdaptiveEnabled
    val canSwitch: Boolean get() = state.canSwitchRefreshRate
    val is60Hz: Boolean get() = state.isPreferring60
}
