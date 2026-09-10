package org.telegram.messenger.feature.leakdetector

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.leakdetector.data.repository.LegacyLeakDetectorRepository
import org.telegram.messenger.feature.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.leakdetector.domain.usecase.ConfirmLeakUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.GetConfirmedLeaksUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.GetTrackedClassesStatsUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.ObserveConfirmedLeaksUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.ObserveLeakDetectorStateUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.ResetLeakDetectorUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.StartLeakDetectionUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.StopLeakDetectionUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.TrackInstanceUseCase
import org.telegram.messenger.feature.leakdetector.domain.usecase.TriggerLeakCheckUseCase
import org.telegram.messenger.feature.leakdetector.presentation.LeakDetectorEvent
import org.telegram.messenger.feature.leakdetector.presentation.LeakDetectorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LeakDetectorDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyLeakDetectorRepository
    private lateinit var startLeakDetectionUseCase: StartLeakDetectionUseCase
    private lateinit var stopLeakDetectionUseCase: StopLeakDetectionUseCase
    private lateinit var trackInstanceUseCase: TrackInstanceUseCase
    private lateinit var triggerLeakCheckUseCase: TriggerLeakCheckUseCase
    private lateinit var confirmLeakUseCase: ConfirmLeakUseCase
    private lateinit var getTrackedClassesStatsUseCase: GetTrackedClassesStatsUseCase
    private lateinit var getConfirmedLeaksUseCase: GetConfirmedLeaksUseCase
    private lateinit var resetLeakDetectorUseCase: ResetLeakDetectorUseCase
    private lateinit var observeLeakDetectorStateUseCase: ObserveLeakDetectorStateUseCase
    private lateinit var observeConfirmedLeaksUseCase: ObserveConfirmedLeaksUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyLeakDetectorRepository(dispatcher = testDispatcher)
        startLeakDetectionUseCase = StartLeakDetectionUseCase(repository)
        stopLeakDetectionUseCase = StopLeakDetectionUseCase(repository)
        trackInstanceUseCase = TrackInstanceUseCase(repository)
        triggerLeakCheckUseCase = TriggerLeakCheckUseCase(repository)
        confirmLeakUseCase = ConfirmLeakUseCase(repository)
        getTrackedClassesStatsUseCase = GetTrackedClassesStatsUseCase(repository)
        getConfirmedLeaksUseCase = GetConfirmedLeaksUseCase(repository)
        resetLeakDetectorUseCase = ResetLeakDetectorUseCase(repository)
        observeLeakDetectorStateUseCase = ObserveLeakDetectorStateUseCase(repository)
        observeConfirmedLeaksUseCase = ObserveConfirmedLeaksUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTrackingAndCountLiveInstances() {
        val obj1 = Any()
        val obj2 = Any()
        val obj3 = Any()

        trackInstanceUseCase("ChatActivity", obj1)
        trackInstanceUseCase("ChatActivity", obj2)
        trackInstanceUseCase("ProfileActivity", obj3)

        val stats = getTrackedClassesStatsUseCase()
        assertEquals(2, stats.size)

        val chatStats = stats.find { it.className == "ChatActivity" }
        assertNotNull(chatStats)
        assertEquals(2, chatStats!!.liveCount)
        assertFalse(chatStats.isSuspicious)

        val profileStats = stats.find { it.className == "ProfileActivity" }
        assertNotNull(profileStats)
        assertEquals(1, profileStats!!.liveCount)
    }

    @Test
    fun testSuspiciousLeakDetectionThreshold() {
        val instances = (1..6).map { Any() }
        for (inst in instances) {
            trackInstanceUseCase("PhotoViewer", inst)
        }

        val stateBefore = repository.observeState().value
        assertEquals(6, stateBefore.totalLiveInstances)

        triggerLeakCheckUseCase()

        val stats = getTrackedClassesStatsUseCase()
        val photoStats = stats.find { it.className == "PhotoViewer" }
        assertNotNull(photoStats)
        assertEquals(6, photoStats!!.liveCount)
        assertTrue(photoStats.isSuspicious)
        assertTrue(photoStats.isPendingRecheck)
    }

    @Test
    fun testTwoPhaseLeakConfirmationAndDeduplication() {
        val instances = (1..5).map { Any() }
        for (inst in instances) {
            trackInstanceUseCase("SecretMediaViewer", inst)
        }

        triggerLeakCheckUseCase()

        // Confirm leak
        val report = confirmLeakUseCase("SecretMediaViewer")
        assertNotNull(report)
        assertEquals("SecretMediaViewer", report!!.className)
        assertEquals(5, report.instanceCount)
        assertEquals(5, report.threshold)

        // Deduplication: second confirmation must return null (already reported)
        val duplicateReport = confirmLeakUseCase("SecretMediaViewer")
        assertNull(duplicateReport)

        val reportedLeaks = getConfirmedLeaksUseCase()
        assertEquals(1, reportedLeaks.size)
        assertEquals("SecretMediaViewer", reportedLeaks[0].className)

        val stats = getTrackedClassesStatsUseCase()
        val viewerStats = stats.find { it.className == "SecretMediaViewer" }
        assertNotNull(viewerStats)
        assertTrue(viewerStats!!.isConfirmedLeak)
    }

    @Test
    fun testStartStopPeriodicScanning() {
        assertFalse(repository.observeState().value.isRunning)

        startLeakDetectionUseCase(LeakDetectorConfig(leakThreshold = 3, checkIntervalMs = 500))
        assertTrue(repository.observeState().value.isRunning)

        stopLeakDetectionUseCase()
        assertFalse(repository.observeState().value.isRunning)
    }

    @Test
    fun testResetLeakDetector() {
        val obj = Any()
        trackInstanceUseCase("TestView", obj)
        assertEquals(1, repository.observeState().value.trackedClassesCount)

        resetLeakDetectorUseCase()

        val state = repository.observeState().value
        assertEquals(0, state.trackedClassesCount)
        assertEquals(0, state.totalLiveInstances)
        assertTrue(state.confirmedLeaks.isEmpty())
        assertTrue(state.pendingRecheckClasses.isEmpty())
    }

    @Test
    fun testLeakDetectorViewModelFlow() = runTest {
        val viewModel = LeakDetectorViewModel(
            startLeakDetectionUseCase = startLeakDetectionUseCase,
            stopLeakDetectionUseCase = stopLeakDetectionUseCase,
            trackInstanceUseCase = trackInstanceUseCase,
            triggerLeakCheckUseCase = triggerLeakCheckUseCase,
            confirmLeakUseCase = confirmLeakUseCase,
            getTrackedClassesStatsUseCase = getTrackedClassesStatsUseCase,
            getConfirmedLeaksUseCase = getConfirmedLeaksUseCase,
            resetLeakDetectorUseCase = resetLeakDetectorUseCase,
            observeLeakDetectorStateUseCase = observeLeakDetectorStateUseCase
        )

        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(0, viewModel.uiState.value.trackedClassesCount)

        // Start detector
        viewModel.onEvent(LeakDetectorEvent.Start())
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isRunning)

        // Track instances
        val objs = (1..5).map { Any() }
        for (o in objs) {
            viewModel.onEvent(LeakDetectorEvent.Track("DialogCell", o))
        }
        testScheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.trackedClassesCount)
        assertEquals(5, viewModel.uiState.value.totalLiveInstances)

        // Trigger check
        viewModel.onEvent(LeakDetectorEvent.TriggerCheck)
        testScheduler.runCurrent()

        // Confirm
        viewModel.onEvent(LeakDetectorEvent.Confirm("DialogCell"))
        testScheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.confirmedLeaks.size)
        assertEquals("DialogCell", viewModel.uiState.value.confirmedLeaks[0].className)

        // Stop & Reset
        viewModel.onEvent(LeakDetectorEvent.Stop)
        viewModel.onEvent(LeakDetectorEvent.Reset)
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(0, viewModel.uiState.value.totalLiveInstances)
    }
}
