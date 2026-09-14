package org.telegram.messenger.feature.system.anrwatchdog.data.datasource

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrSeverity
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogConfig
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AppLifecycleState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.PingRecord
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Local data source encapsulating thread-safe watchdog heartbeat monitoring,
 * freeze detection calculation, and incident lifecycle management.
 */
class AnrWatchdogLocalDataSource(
    private val config: AnrWatchdogConfig = AnrWatchdogConfig()
) {
    private val lock = Any()
    private val idCounter = AtomicLong(0L)

    private var isRunning: Boolean = false
    private var lifecycleState: AppLifecycleState = AppLifecycleState.FOREGROUND
    private var generation: Int = 0
    private var acknowledgedPingId: Long = -1L
    private var anrReported: Boolean = false
    private var currentIncident: AnrIncident? = null

    private val pendingPings = LinkedHashMap<Long, PingRecord>()
    private val incidentHistory = mutableListOf<AnrIncident>()

    private val _state = MutableStateFlow(AnrWatchdogState())
    val state: StateFlow<AnrWatchdogState> = _state.asStateFlow()

    private val _incidents = MutableSharedFlow<AnrIncident>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incidents: Flow<AnrIncident> = _incidents.asSharedFlow()

    fun startMonitoring() = synchronized(lock) {
        isRunning = true
        lifecycleState = AppLifecycleState.FOREGROUND
        updateStateLocked()
    }

    fun stopMonitoring() = synchronized(lock) {
        isRunning = false
        lifecycleState = AppLifecycleState.STOPPED
        generation++
        pendingPings.clear()
        anrReported = false
        currentIncident = null
        updateStateLocked()
    }

    fun setForeground(isForeground: Boolean) = synchronized(lock) {
        generation++
        if (isForeground) {
            lifecycleState = AppLifecycleState.FOREGROUND
            anrReported = false
        } else {
            lifecycleState = AppLifecycleState.BACKGROUND
            pendingPings.clear()
        }
        updateStateLocked()
    }

    fun sendPing(timestampNanos: Long = System.nanoTime()): PingRecord = synchronized(lock) {
        val pingId = idCounter.incrementAndGet()
        val record = PingRecord(
            id = pingId,
            generation = generation,
            sentAtNanos = timestampNanos
        )
        pendingPings[pingId] = record
        updateStateLocked()
        record
    }

    fun acknowledgePing(pingId: Long) = synchronized(lock) {
        acknowledgedPingId = pingId
        pendingPings.remove(pingId)

        if (anrReported || currentIncident != null) {
            anrReported = false
            currentIncident = currentIncident?.copy(isRecovered = true)
        }
        updateStateLocked()
    }

    fun checkFreeze(nowNanos: Long = System.nanoTime(), timeoutMs: Long = config.timeoutMs): AnrIncident? {
        var newIncident: AnrIncident? = null

        synchronized(lock) {
            if (!isRunning || lifecycleState != AppLifecycleState.FOREGROUND) {
                return null
            }

            val currentGen = generation
            val oldest = pendingPings.values.firstOrNull { it.generation == currentGen } ?: return null

            val elapsedMs = (nowNanos - oldest.sentAtNanos) / 1_000_000L
            if (elapsedMs >= timeoutMs && !anrReported) {
                anrReported = true
                val incident = AnrIncident(
                    incidentId = UUID.randomUUID().toString(),
                    pingId = oldest.id,
                    freezeDurationMs = elapsedMs,
                    generation = currentGen,
                    timestampMs = System.currentTimeMillis(),
                    severity = if (elapsedMs >= 5000L) AnrSeverity.CRITICAL else AnrSeverity.WARNING,
                    isRecovered = false
                )
                currentIncident = incident
                incidentHistory.add(incident)
                newIncident = incident
                updateStateLocked()
            }
        }

        newIncident?.let {
            _incidents.tryEmit(it)
        }
        return newIncident
    }

    fun resolveIncident(incidentId: String) = synchronized(lock) {
        if (currentIncident?.incidentId == incidentId) {
            currentIncident = currentIncident?.copy(isRecovered = true)
            anrReported = false
            updateStateLocked()
        }
    }

    fun getState(): AnrWatchdogState = synchronized(lock) {
        _state.value
    }

    fun getIncidentHistory(): List<AnrIncident> = synchronized(lock) {
        incidentHistory.toList()
    }

    fun clearHistory() = synchronized(lock) {
        incidentHistory.clear()
        updateStateLocked()
    }

    fun observeState(): StateFlow<AnrWatchdogState> = state

    fun observeIncidents(): Flow<AnrIncident> = incidents

    private fun updateStateLocked() {
        val lastPing = pendingPings.keys.lastOrNull() ?: acknowledgedPingId
        val frozen = anrReported || (currentIncident != null && !currentIncident!!.isRecovered)

        _state.value = AnrWatchdogState(
            isRunning = isRunning,
            lifecycleState = lifecycleState,
            currentGeneration = generation,
            lastSentPingId = lastPing,
            lastAcknowledgedPingId = acknowledgedPingId,
            isFrozen = frozen,
            anrReported = anrReported,
            reportedAnrsCount = incidentHistory.size,
            activeIncident = currentIncident
        )
    }
}
