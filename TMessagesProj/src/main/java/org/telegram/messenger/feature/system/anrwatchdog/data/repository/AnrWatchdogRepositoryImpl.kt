package org.telegram.messenger.feature.system.anrwatchdog.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.anrwatchdog.data.datasource.AnrWatchdogLocalDataSource
import org.telegram.messenger.feature.system.anrwatchdog.data.datasource.AnrWatchdogRemoteDataSource
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogState
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.PingRecord
import org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository

/**
 * Implementation of [AnrWatchdogRepository] coordinating local heartbeat state and remote reporting.
 */
class AnrWatchdogRepositoryImpl(
    private val localDataSource: AnrWatchdogLocalDataSource,
    private val remoteDataSource: AnrWatchdogRemoteDataSource
) : AnrWatchdogRepository {

    override fun startMonitoring() {
        localDataSource.startMonitoring()
    }

    override fun stopMonitoring() {
        localDataSource.stopMonitoring()
    }

    override fun setForeground(isForeground: Boolean) {
        localDataSource.setForeground(isForeground)
    }

    override fun sendPing(timestampNanos: Long): PingRecord {
        return localDataSource.sendPing(timestampNanos)
    }

    override fun acknowledgePing(pingId: Long) {
        localDataSource.acknowledgePing(pingId)
    }

    override fun checkFreeze(nowNanos: Long, timeoutMs: Long): AnrIncident? {
        val incident = localDataSource.checkFreeze(nowNanos, timeoutMs)
        return incident
    }

    override fun resolveIncident(incidentId: String) {
        localDataSource.resolveIncident(incidentId)
    }

    override fun getState(): AnrWatchdogState {
        return localDataSource.getState()
    }

    override fun getIncidentHistory(): List<AnrIncident> {
        return localDataSource.getIncidentHistory()
    }

    override fun clearHistory() {
        localDataSource.clearHistory()
    }

    override fun observeState(): StateFlow<AnrWatchdogState> {
        return localDataSource.observeState()
    }

    override fun observeIncidents(): Flow<AnrIncident> {
        return localDataSource.observeIncidents()
    }
}
