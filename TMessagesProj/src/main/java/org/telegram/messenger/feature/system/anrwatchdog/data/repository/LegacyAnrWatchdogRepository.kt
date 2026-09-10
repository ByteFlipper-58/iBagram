package org.telegram.messenger.feature.system.anrwatchdog.data.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.anrwatchdog.data.mapper.AnrWatchdogMapper
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AppLifecycleState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.PingRecord
import org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe implementation of [AnrWatchdogRepository] isolating the logic of
 * [org.telegram.messenger.ANRDetector].
 */
class LegacyAnrWatchdogRepository : AnrWatchdogRepository {

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
    private val _incidents = MutableSharedFlow<AnrIncident>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun startMonitoring() = synchronized(lock) {
        isRunning = true
        lifecycleState = AppLifecycleState.FOREGROUND
        updateStateLocked()
    }

    override fun stopMonitoring() = synchronized(lock) {
        isRunning = false
        lifecycleState = AppLifecycleState.STOPPED
        generation++
        pendingPings.clear()
        anrReported = false
        currentIncident = null
        updateStateLocked()
    }

    override fun setForeground(isForeground: Boolean) = synchronized(lock) {
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

    override fun sendPing(timestampNanos: Long): PingRecord = synchronized(lock) {
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

    override fun acknowledgePing(pingId: Long) = synchronized(lock) {
        acknowledgedPingId = pingId
        pendingPings.remove(pingId)

        // If previously frozen, mark recovered
        if (anrReported || currentIncident != null) {
            anrReported = false
            currentIncident = currentIncident?.copy(isRecovered = true)
        }
        updateStateLocked()
    }

    override fun checkFreeze(nowNanos: Long, timeoutMs: Long): AnrIncident? {
        var newIncident: AnrIncident? = null

        synchronized(lock) {
            if (!isRunning || lifecycleState != AppLifecycleState.FOREGROUND) {
                return null
            }

            // Look at pending pings for the current generation
            val pending = pendingPings.values.firstOrNull { it.generation == generation } ?: return null

            if (acknowledgedPingId >= pending.id) {
                return null
            }

            val elapsedMs = (nowNanos - pending.sentAtNanos) / 1_000_000L
            if (elapsedMs >= timeoutMs) {
                if (!anrReported) {
                    anrReported = true
                    val incident = AnrWatchdogMapper.createIncident(
                        pingId = pending.id,
                        freezeDurationMs = elapsedMs,
                        generation = generation
                    )
                    currentIncident = incident
                    incidentHistory.add(incident)
                    newIncident = incident
                    updateStateLocked()
                }
            }
        }

        newIncident?.let {
            _incidents.tryEmit(it)
        }

        return newIncident
    }

    override fun resolveIncident(incidentId: String) = synchronized(lock) {
        if (currentIncident?.incidentId == incidentId) {
            currentIncident = currentIncident?.copy(isRecovered = true)
            anrReported = false
            updateStateLocked()
        }
    }

    override fun getState(): AnrWatchdogState = synchronized(lock) {
        _state.value
    }

    override fun getIncidentHistory(): List<AnrIncident> = synchronized(lock) {
        ArrayList(incidentHistory)
    }

    override fun clearHistory() = synchronized(lock) {
        incidentHistory.clear()
        updateStateLocked()
    }

    override fun observeState(): StateFlow<AnrWatchdogState> = _state.asStateFlow()

    override fun observeIncidents(): Flow<AnrIncident> = _incidents.asSharedFlow()

    private fun updateStateLocked() {
        _state.value = AnrWatchdogState(
            isRunning = isRunning,
            lifecycleState = lifecycleState,
            currentGeneration = generation,
            lastSentPingId = idCounter.get(),
            lastAcknowledgedPingId = acknowledgedPingId,
            isFrozen = anrReported,
            anrReported = anrReported,
            reportedAnrsCount = incidentHistory.size,
            activeIncident = currentIncident
        )
    }
}
