package org.telegram.messenger.feature.system.anrwatchdog.domain.model

/**
 * Severity level of main thread unresponsive state.
 */
enum class AnrSeverity {
    WARNING,  // UI freeze > 2000ms
    CRITICAL  // Full ANR freeze > 5000ms
}

/**
 * Current lifecycle foreground/background state.
 */
enum class AppLifecycleState {
    FOREGROUND,
    BACKGROUND,
    STOPPED
}

/**
 * Record of a ping message dispatched to the main UI looper.
 */
data class PingRecord(
    val id: Long,
    val generation: Int,
    val sentAtNanos: Long
)

/**
 * Detailed report of an ANR or main thread freeze incident.
 */
data class AnrIncident(
    val incidentId: String,
    val pingId: Long,
    val freezeDurationMs: Long,
    val generation: Int,
    val timestampMs: Long,
    val severity: AnrSeverity = if (freezeDurationMs >= 5000L) AnrSeverity.CRITICAL else AnrSeverity.WARNING,
    val isRecovered: Boolean = false,
    val threadSummary: String = "Main thread unresponsive for ${freezeDurationMs}ms"
)

/**
 * Configuration options for ANR watchdog detection.
 */
data class AnrWatchdogConfig(
    val timeoutMs: Long = 5000L,
    val pingIntervalMs: Long = 5000L
)

/**
 * Snapshot of ANR watchdog state.
 */
data class AnrWatchdogState(
    val isRunning: Boolean = false,
    val lifecycleState: AppLifecycleState = AppLifecycleState.FOREGROUND,
    val currentGeneration: Int = 0,
    val lastSentPingId: Long = 0L,
    val lastAcknowledgedPingId: Long = -1L,
    val isFrozen: Boolean = false,
    val anrReported: Boolean = false,
    val reportedAnrsCount: Int = 0,
    val activeIncident: AnrIncident? = null
) {
    val isForeground: Boolean get() = lifecycleState == AppLifecycleState.FOREGROUND
}
