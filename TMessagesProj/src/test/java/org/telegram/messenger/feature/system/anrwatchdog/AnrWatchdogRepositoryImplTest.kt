package org.telegram.messenger.feature.system.anrwatchdog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.anrwatchdog.data.datasource.AnrWatchdogLocalDataSource
import org.telegram.messenger.feature.system.anrwatchdog.data.datasource.AnrWatchdogRemoteDataSource
import org.telegram.messenger.feature.system.anrwatchdog.data.repository.AnrWatchdogRepositoryImpl
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrSeverity
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrWatchdogConfig
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AppLifecycleState

class AnrWatchdogRepositoryImplTest {

    private lateinit var localDataSource: AnrWatchdogLocalDataSource
    private lateinit var remoteDataSource: AnrWatchdogRemoteDataSource
    private lateinit var repository: AnrWatchdogRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AnrWatchdogLocalDataSource(config = AnrWatchdogConfig(timeoutMs = 5000L))
        remoteDataSource = AnrWatchdogRemoteDataSource(currentAccount = 0)
        repository = AnrWatchdogRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialState_isStoppedAndNotFrozen() {
        val state = repository.getState()
        assertFalse(state.isRunning)
        assertFalse(state.isFrozen)
        assertFalse(state.anrReported)
        assertEquals(0, state.reportedAnrsCount)
        assertNull(state.activeIncident)
        assertTrue(repository.getIncidentHistory().isEmpty())
    }

    @Test
    fun startMonitoring_setsRunningAndForeground() {
        repository.startMonitoring()
        val state = repository.getState()
        assertTrue(state.isRunning)
        assertEquals(AppLifecycleState.FOREGROUND, state.lifecycleState)
        assertTrue(state.isForeground)

        repository.stopMonitoring()
        assertFalse(repository.getState().isRunning)
        assertEquals(AppLifecycleState.STOPPED, repository.getState().lifecycleState)
    }

    @Test
    fun setForeground_togglesLifecycleAndClearsPings() {
        repository.startMonitoring()
        repository.sendPing(timestampNanos = 1000L)

        repository.setForeground(false)
        val stateBg = repository.getState()
        assertEquals(AppLifecycleState.BACKGROUND, stateBg.lifecycleState)
        assertFalse(stateBg.isForeground)

        repository.setForeground(true)
        val stateFg = repository.getState()
        assertEquals(AppLifecycleState.FOREGROUND, stateFg.lifecycleState)
        assertTrue(stateFg.isForeground)
    }

    @Test
    fun sendPingAndAcknowledge_updatesCountersAndClearsPending() {
        repository.startMonitoring()
        val ping = repository.sendPing(timestampNanos = 100_000_000L)
        assertTrue(ping.id > 0)
        assertEquals(ping.id, repository.getState().lastSentPingId)

        repository.acknowledgePing(ping.id)
        assertEquals(ping.id, repository.getState().lastAcknowledgedPingId)
        assertFalse(repository.getState().isFrozen)
    }

    @Test
    fun checkFreeze_detectsFreezeWhenPingTimesOut() {
        repository.startMonitoring()
        val sentNanos = 100_000_000L
        val ping = repository.sendPing(timestampNanos = sentNanos)

        // 2 seconds later: no freeze (< 5000ms)
        val checkNormal = repository.checkFreeze(nowNanos = sentNanos + 2_000_000_000L, timeoutMs = 5000L)
        assertNull(checkNormal)
        assertFalse(repository.getState().isFrozen)

        // 6 seconds later: freeze detected (> 5000ms)
        val incident = repository.checkFreeze(nowNanos = sentNanos + 6_000_000_000L, timeoutMs = 5000L)
        assertNotNull(incident)
        assertEquals(ping.id, incident!!.pingId)
        assertEquals(6000L, incident.freezeDurationMs)
        assertEquals(AnrSeverity.CRITICAL, incident.severity)
        assertFalse(incident.isRecovered)

        assertTrue(repository.getState().isFrozen)
        assertTrue(repository.getState().anrReported)
        assertEquals(1, repository.getState().reportedAnrsCount)
        assertEquals(1, repository.getIncidentHistory().size)

        // Repeat check does not re-report the same freeze
        val duplicateCheck = repository.checkFreeze(nowNanos = sentNanos + 7_000_000_000L, timeoutMs = 5000L)
        assertNull(duplicateCheck)
    }

    @Test
    fun resolveIncident_marksIncidentRecovered() {
        repository.startMonitoring()
        val sentNanos = 100_000_000L
        repository.sendPing(timestampNanos = sentNanos)

        val incident = repository.checkFreeze(nowNanos = sentNanos + 6_000_000_000L, timeoutMs = 5000L)
        assertNotNull(incident)
        assertTrue(repository.getState().isFrozen)

        repository.resolveIncident(incident!!.incidentId)
        assertFalse(repository.getState().isFrozen)
        assertTrue(repository.getState().activeIncident?.isRecovered == true)
    }

    @Test
    fun clearHistory_removesPastIncidents() {
        repository.startMonitoring()
        repository.sendPing(timestampNanos = 100_000_000L)
        repository.checkFreeze(nowNanos = 6_100_000_000L, timeoutMs = 5000L)
        assertEquals(1, repository.getIncidentHistory().size)

        repository.clearHistory()
        assertTrue(repository.getIncidentHistory().isEmpty())
        assertEquals(0, repository.getState().reportedAnrsCount)
    }
}
