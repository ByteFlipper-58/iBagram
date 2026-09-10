package org.telegram.messenger.feature.system.anrwatchdog.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.PingRecord
import org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository

/**
 * Starts ANR watchdog diagnostics.
 */
class StartAnrMonitoringUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke() {
        repository.startMonitoring()
    }
}

/**
 * Stops ANR watchdog diagnostics.
 */
class StopAnrMonitoringUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke() {
        repository.stopMonitoring()
    }
}

/**
 * Informs watchdog about app foreground/background transition.
 */
class SetAppForegroundStatusUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(isForeground: Boolean) {
        repository.setForeground(isForeground)
    }
}

/**
 * Dispatches a ping message intended for the main thread.
 */
class SendMainThreadPingUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(timestampNanos: Long = System.nanoTime()): PingRecord {
        return repository.sendPing(timestampNanos)
    }
}

/**
 * Acknowledges that the main thread processed a ping message.
 */
class AcknowledgePingUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(pingId: Long) {
        repository.acknowledgePing(pingId)
    }
}

/**
 * Checks whether the pending ping timed out and returns an incident if freeze occurred.
 */
class CheckMainThreadFreezeUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(nowNanos: Long = System.nanoTime(), timeoutMs: Long = 5000L): AnrIncident? {
        return repository.checkFreeze(nowNanos, timeoutMs)
    }
}

/**
 * Resolves an active freeze incident.
 */
class ResolveIncidentUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(incidentId: String) {
        repository.resolveIncident(incidentId)
    }
}

/**
 * Retrieves current watchdog status snapshot.
 */
class GetAnrWatchdogStateUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(): AnrWatchdogState {
        return repository.getState()
    }
}

/**
 * Retrieves past recorded freeze incidents.
 */
class GetAnrIncidentsUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(): List<AnrIncident> {
        return repository.getIncidentHistory()
    }
}

/**
 * Clears incident history.
 */
class ClearAnrHistoryUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke() {
        repository.clearHistory()
    }
}

/**
 * Observes real-time watchdog state stream.
 */
class ObserveAnrWatchdogStateUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(): StateFlow<AnrWatchdogState> {
        return repository.observeState()
    }
}

/**
 * Observes stream of detected ANR incidents.
 */
class ObserveAnrIncidentsUseCase(private val repository: AnrWatchdogRepository) {
    operator fun invoke(): Flow<AnrIncident> {
        return repository.observeIncidents()
    }
}
