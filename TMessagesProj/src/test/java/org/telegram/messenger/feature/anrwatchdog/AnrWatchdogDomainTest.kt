package org.telegram.messenger.feature.anrwatchdog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.anrwatchdog.data.repository.LegacyAnrWatchdogRepository
import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrSeverity
import org.telegram.messenger.feature.anrwatchdog.domain.model.AppLifecycleState
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.AcknowledgePingUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.CheckMainThreadFreezeUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ClearAnrHistoryUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.GetAnrIncidentsUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.GetAnrWatchdogStateUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ObserveAnrIncidentsUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ObserveAnrWatchdogStateUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.ResolveIncidentUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.SendMainThreadPingUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.SetAppForegroundStatusUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.StartAnrMonitoringUseCase
import org.telegram.messenger.feature.anrwatchdog.domain.usecase.StopAnrMonitoringUseCase
import org.telegram.messenger.feature.anrwatchdog.presentation.AnrWatchdogEvent
import org.telegram.messenger.feature.anrwatchdog.presentation.AnrWatchdogViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AnrWatchdogDomainTest {

    private lateinit var repository: LegacyAnrWatchdogRepository

    private lateinit var startAnrMonitoringUseCase: StartAnrMonitoringUseCase
    private lateinit var stopAnrMonitoringUseCase: StopAnrMonitoringUseCase
    private lateinit var setAppForegroundStatusUseCase: SetAppForegroundStatusUseCase
    private lateinit var sendMainThreadPingUseCase: SendMainThreadPingUseCase
    private lateinit var acknowledgePingUseCase: AcknowledgePingUseCase
    private lateinit var checkMainThreadFreezeUseCase: CheckMainThreadFreezeUseCase
    private lateinit var resolveIncidentUseCase: ResolveIncidentUseCase
    private lateinit var getAnrWatchdogStateUseCase: GetAnrWatchdogStateUseCase
    private lateinit var getAnrIncidentsUseCase: GetAnrIncidentsUseCase
    private lateinit var clearAnrHistoryUseCase: ClearAnrHistoryUseCase
    private lateinit var observeAnrWatchdogStateUseCase: ObserveAnrWatchdogStateUseCase
    private lateinit var observeAnrIncidentsUseCase: ObserveAnrIncidentsUseCase

    @Before
    fun setUp() {
        repository = LegacyAnrWatchdogRepository()

        startAnrMonitoringUseCase = StartAnrMonitoringUseCase(repository)
        stopAnrMonitoringUseCase = StopAnrMonitoringUseCase(repository)
        setAppForegroundStatusUseCase = SetAppForegroundStatusUseCase(repository)
        sendMainThreadPingUseCase = SendMainThreadPingUseCase(repository)
        acknowledgePingUseCase = AcknowledgePingUseCase(repository)
        checkMainThreadFreezeUseCase = CheckMainThreadFreezeUseCase(repository)
        resolveIncidentUseCase = ResolveIncidentUseCase(repository)
        getAnrWatchdogStateUseCase = GetAnrWatchdogStateUseCase(repository)
        getAnrIncidentsUseCase = GetAnrIncidentsUseCase(repository)
        clearAnrHistoryUseCase = ClearAnrHistoryUseCase(repository)
        observeAnrWatchdogStateUseCase = ObserveAnrWatchdogStateUseCase(repository)
        observeAnrIncidentsUseCase = ObserveAnrIncidentsUseCase(repository)
    }

    @Test
    fun testStartAndStopMonitoring() {
        assertFalse(getAnrWatchdogStateUseCase().isRunning)

        startAnrMonitoringUseCase()
        val stateStarted = getAnrWatchdogStateUseCase()
        assertTrue(stateStarted.isRunning)
        assertEquals(AppLifecycleState.FOREGROUND, stateStarted.lifecycleState)

        stopAnrMonitoringUseCase()
        val stateStopped = getAnrWatchdogStateUseCase()
        assertFalse(stateStopped.isRunning)
        assertEquals(AppLifecycleState.STOPPED, stateStopped.lifecycleState)
    }

    @Test
    fun testPingAndAcknowledgment() {
        startAnrMonitoringUseCase()

        val ping = sendMainThreadPingUseCase(1_000_000_000L)
        assertEquals(1L, ping.id)

        val stateAfterPing = getAnrWatchdogStateUseCase()
        assertEquals(1L, stateAfterPing.lastSentPingId)
        assertEquals(-1L, stateAfterPing.lastAcknowledgedPingId)

        acknowledgePingUseCase(ping.id)
        val stateAfterAck = getAnrWatchdogStateUseCase()
        assertEquals(1L, stateAfterAck.lastAcknowledgedPingId)
        assertFalse(stateAfterAck.isFrozen)
    }

    @Test
    fun testFreezeDetectionAndTimeout() {
        startAnrMonitoringUseCase()

        val startNanos = 1_000_000_000L
        val ping = sendMainThreadPingUseCase(startNanos)

        // 1. Check before timeout (2000ms elapsed) -> no ANR
        val incidentBefore = checkMainThreadFreezeUseCase(
            nowNanos = startNanos + 2_000_000_000L,
            timeoutMs = 5000L
        )
        assertNull(incidentBefore)
        assertFalse(getAnrWatchdogStateUseCase().isFrozen)

        // 2. Check after timeout (5500ms elapsed) -> ANR detected!
        val incidentAfter = checkMainThreadFreezeUseCase(
            nowNanos = startNanos + 5_500_000_000L,
            timeoutMs = 5000L
        )
        assertNotNull(incidentAfter)
        assertEquals(ping.id, incidentAfter!!.pingId)
        assertEquals(5500L, incidentAfter.freezeDurationMs)
        assertEquals(AnrSeverity.CRITICAL, incidentAfter.severity)

        val stateFrozen = getAnrWatchdogStateUseCase()
        assertTrue(stateFrozen.isFrozen)
        assertTrue(stateFrozen.anrReported)
        assertEquals(1, stateFrozen.reportedAnrsCount)

        // 3. Check again while still frozen -> deduplicated (null)
        val incidentDuplicate = checkMainThreadFreezeUseCase(
            nowNanos = startNanos + 6_000_000_000L,
            timeoutMs = 5000L
        )
        assertNull(incidentDuplicate)
    }

    @Test
    fun testMainThreadRecovery() {
        startAnrMonitoringUseCase()

        val startNanos = 2_000_000_000L
        val ping = sendMainThreadPingUseCase(startNanos)

        // Trigger freeze
        val incident = checkMainThreadFreezeUseCase(
            nowNanos = startNanos + 5_200_000_000L,
            timeoutMs = 5000L
        )
        assertNotNull(incident)
        assertTrue(getAnrWatchdogStateUseCase().isFrozen)

        // Main thread recovers and acknowledges the ping
        acknowledgePingUseCase(ping.id)

        val stateRecovered = getAnrWatchdogStateUseCase()
        assertFalse(stateRecovered.isFrozen)
        assertFalse(stateRecovered.anrReported)
        assertTrue(stateRecovered.activeIncident?.isRecovered == true)
    }

    @Test
    fun testForegroundBackgroundTransition() {
        startAnrMonitoringUseCase()

        // Transition to background
        setAppForegroundStatusUseCase(false)
        val stateBg = getAnrWatchdogStateUseCase()
        assertEquals(AppLifecycleState.BACKGROUND, stateBg.lifecycleState)
        assertFalse(stateBg.isForeground)

        // While in background, pings do not produce ANRs
        val incidentBg = checkMainThreadFreezeUseCase(
            nowNanos = 10_000_000_000L,
            timeoutMs = 5000L
        )
        assertNull(incidentBg)

        // Back to foreground
        setAppForegroundStatusUseCase(true)
        val stateFg = getAnrWatchdogStateUseCase()
        assertEquals(AppLifecycleState.FOREGROUND, stateFg.lifecycleState)
        assertTrue(stateFg.isForeground)
    }

    @Test
    fun testAnrWatchdogViewModelFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = AnrWatchdogViewModel(
            startAnrMonitoringUseCase = startAnrMonitoringUseCase,
            stopAnrMonitoringUseCase = stopAnrMonitoringUseCase,
            setAppForegroundStatusUseCase = setAppForegroundStatusUseCase,
            sendMainThreadPingUseCase = sendMainThreadPingUseCase,
            acknowledgePingUseCase = acknowledgePingUseCase,
            checkMainThreadFreezeUseCase = checkMainThreadFreezeUseCase,
            resolveIncidentUseCase = resolveIncidentUseCase,
            getAnrWatchdogStateUseCase = getAnrWatchdogStateUseCase,
            getAnrIncidentsUseCase = getAnrIncidentsUseCase,
            clearAnrHistoryUseCase = clearAnrHistoryUseCase,
            observeAnrWatchdogStateUseCase = observeAnrWatchdogStateUseCase,
            observeAnrIncidentsUseCase = observeAnrIncidentsUseCase,
            scope = testScope
        )

        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isRunning)

        // 1. Start monitoring
        viewModel.onEvent(AnrWatchdogEvent.StartMonitoring)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isRunning)

        // 2. Send ping & freeze
        val startNanos = 5_000_000_000L
        viewModel.onEvent(AnrWatchdogEvent.SendPing(startNanos))
        testScheduler.runCurrent()

        viewModel.onEvent(
            AnrWatchdogEvent.CheckFreeze(
                nowNanos = startNanos + 5_500_000_000L,
                timeoutMs = 5000L
            )
        )
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isFrozen)
        assertEquals(1, viewModel.uiState.value.incidents.size)

        // 3. Acknowledge and clear
        viewModel.onEvent(AnrWatchdogEvent.AcknowledgePing(1L))
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isFrozen)

        viewModel.onEvent(AnrWatchdogEvent.ClearHistory)
        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.incidents.size)

        viewModel.onCleared()
    }
}
