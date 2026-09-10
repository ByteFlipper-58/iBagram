package org.telegram.messenger.feature.leakdetector.presentation

import org.telegram.messenger.feature.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.leakdetector.domain.model.TrackedClassStats

/**
 * UI State for memory leak detection dashboard/monitor.
 */
data class LeakDetectorUiState(
    val isRunning: Boolean = false,
    val trackedClassesCount: Int = 0,
    val totalLiveInstances: Int = 0,
    val suspiciousClassesCount: Int = 0,
    val confirmedLeaks: List<LeakReport> = emptyList(),
    val trackedStats: List<TrackedClassStats> = emptyList(),
    val isChecking: Boolean = false,
    val errorMessage: String? = null
)
