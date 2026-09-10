package org.telegram.messenger.feature.system.anrwatchdog.presentation

/**
 * MVI Events for ANR watchdog monitoring.
 */
sealed class AnrWatchdogEvent {
    object StartMonitoring : AnrWatchdogEvent()
    object StopMonitoring : AnrWatchdogEvent()
    data class SetForeground(val isForeground: Boolean) : AnrWatchdogEvent()
    data class SendPing(val timestampNanos: Long = System.nanoTime()) : AnrWatchdogEvent()
    data class AcknowledgePing(val pingId: Long) : AnrWatchdogEvent()
    data class CheckFreeze(val nowNanos: Long = System.nanoTime(), val timeoutMs: Long = 5000L) : AnrWatchdogEvent()
    data class ResolveIncident(val incidentId: String) : AnrWatchdogEvent()
    object ClearHistory : AnrWatchdogEvent()
    object DismissError : AnrWatchdogEvent()
}
