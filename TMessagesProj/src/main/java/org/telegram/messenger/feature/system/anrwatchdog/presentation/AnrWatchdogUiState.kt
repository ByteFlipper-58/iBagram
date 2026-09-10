package org.telegram.messenger.feature.system.anrwatchdog.presentation

import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogState

/**
 * UI State for ANR watchdog monitoring and diagnostics dashboard.
 */
data class AnrWatchdogUiState(
    val state: AnrWatchdogState = AnrWatchdogState(),
    val incidents: List<AnrIncident> = emptyList(),
    val isChecking: Boolean = false,
    val errorMessage: String? = null
) {
    val isRunning: Boolean get() = state.isRunning
    val isForeground: Boolean get() = state.isForeground
    val isFrozen: Boolean get() = state.isFrozen
    val reportedAnrsCount: Int get() = state.reportedAnrsCount
    val activeIncident: AnrIncident? get() = state.activeIncident
}
