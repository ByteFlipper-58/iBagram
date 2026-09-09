package org.telegram.messenger.feature.refreshrate.presentation

import org.telegram.messenger.feature.refreshrate.domain.model.DisplayRefreshModeModel

/**
 * UI intents for display refresh rate feature.
 */
sealed interface RefreshRateEvent {
    data object StartTracking : RefreshRateEvent
    data object StopTracking : RefreshRateEvent
    data class ToggleAdaptive(val enabled: Boolean) : RefreshRateEvent
    data class SelectMode(val mode: DisplayRefreshModeModel) : RefreshRateEvent
    data class RecordFrame(val durationNs: Long) : RefreshRateEvent
    data object ResetStats : RefreshRateEvent
    data object DismissInfo : RefreshRateEvent
}
