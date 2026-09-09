package org.telegram.messenger.feature.refreshrate.domain.model

/**
 * Pure domain model representing current refresh rate tracking and display state.
 */
data class RefreshRateStateModel(
    val isTrackingActive: Boolean = false,
    val isAdaptiveEnabled: Boolean = true,
    val currentMode: DisplayRefreshModeModel? = null,
    val mode60: DisplayRefreshModeModel? = null,
    val modeMax: DisplayRefreshModeModel? = null,
    val isPreferring60: Boolean = false,
    val currentFps: Float = 60.0f,
    val totalFramesTracked: Long = 0L,
    val lastDirection: RefreshRateDirection = RefreshRateDirection.NONE,
    val availableModes: List<DisplayRefreshModeModel> = emptyList()
) {
    val canSwitchRefreshRate: Boolean
        get() = mode60 != null && modeMax != null && mode60.modeId != modeMax.modeId
}
