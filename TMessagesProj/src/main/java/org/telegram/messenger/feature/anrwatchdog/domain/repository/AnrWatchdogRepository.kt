package org.telegram.messenger.feature.anrwatchdog.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrWatchdogState
import org.telegram.messenger.feature.anrwatchdog.domain.model.PingRecord

/**
 * Repository interface for managing ANR watchdog diagnostics,
 * main thread ping dispatch, freeze detection, and incident reporting.
 */
interface AnrWatchdogRepository {

    /**
     * Starts watchdog monitoring.
     */
    fun startMonitoring()

    /**
     * Stops watchdog monitoring and releases resources.
     */
    fun stopMonitoring()

    /**
     * Informs the watchdog about foreground/background transitions.
     */
    fun setForeground(isForeground: Boolean)

    /**
     * Dispatches a ping message intended for the main thread.
     */
    fun sendPing(timestampNanos: Long = System.nanoTime()): PingRecord

    /**
     * Acknowledges that the main thread has processed a ping message.
     */
    fun acknowledgePing(pingId: Long)

    /**
     * Checks if the pending ping timed out and returns an incident if freeze is detected.
     */
    fun checkFreeze(nowNanos: Long = System.nanoTime(), timeoutMs: Long = 5000L): AnrIncident?

    /**
     * Marks an incident as recovered once the main thread resumes processing.
     */
    fun resolveIncident(incidentId: String)

    /**
     * Retrieves current watchdog status.
     */
    fun getState(): AnrWatchdogState

    /**
     * Retrieves past recorded freeze incidents.
     */
    fun getIncidentHistory(): List<AnrIncident>

    /**
     * Clears all recorded incidents.
     */
    fun clearHistory()

    /**
     * Observes real-time watchdog state changes.
     */
    fun observeState(): StateFlow<AnrWatchdogState>

    /**
     * Hot stream of detected ANR incidents.
     */
    fun observeIncidents(): Flow<AnrIncident>
}
