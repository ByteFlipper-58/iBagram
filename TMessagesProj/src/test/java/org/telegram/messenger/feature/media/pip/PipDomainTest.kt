package org.telegram.messenger.feature.media.pip

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
import org.telegram.messenger.feature.media.pip.data.repository.LegacyPipRepository
import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState
import org.telegram.messenger.feature.media.pip.domain.usecase.DispatchPipStateUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.EvaluatePipEligibilityUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.GetPipSessionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.ObservePipSessionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.RegisterPipSourceUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.TriggerPipActionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.UnregisterPipSourceUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.UpdatePipSourceStateUseCase
import org.telegram.messenger.feature.media.pip.presentation.PipEvent
import org.telegram.messenger.feature.media.pip.presentation.PipViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PipDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyPipRepository
    private lateinit var observePipSessionUseCase: ObservePipSessionUseCase
    private lateinit var getPipSessionUseCase: GetPipSessionUseCase
    private lateinit var registerPipSourceUseCase: RegisterPipSourceUseCase
    private lateinit var unregisterPipSourceUseCase: UnregisterPipSourceUseCase
    private lateinit var updatePipSourceStateUseCase: UpdatePipSourceStateUseCase
    private lateinit var dispatchPipStateUseCase: DispatchPipStateUseCase
    private lateinit var triggerPipActionUseCase: TriggerPipActionUseCase
    private lateinit var evaluatePipEligibilityUseCase: EvaluatePipEligibilityUseCase
    private lateinit var viewModel: PipViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyPipRepository()
        observePipSessionUseCase = ObservePipSessionUseCase(repository)
        getPipSessionUseCase = GetPipSessionUseCase(repository)
        registerPipSourceUseCase = RegisterPipSourceUseCase(repository)
        unregisterPipSourceUseCase = UnregisterPipSourceUseCase(repository)
        updatePipSourceStateUseCase = UpdatePipSourceStateUseCase(repository)
        dispatchPipStateUseCase = DispatchPipStateUseCase(repository)
        triggerPipActionUseCase = TriggerPipActionUseCase(repository)
        evaluatePipEligibilityUseCase = EvaluatePipEligibilityUseCase(repository)

        viewModel = PipViewModel(
            observePipSessionUseCase = observePipSessionUseCase,
            getPipSessionUseCase = getPipSessionUseCase,
            registerPipSourceUseCase = registerPipSourceUseCase,
            unregisterPipSourceUseCase = unregisterPipSourceUseCase,
            updatePipSourceStateUseCase = updatePipSourceStateUseCase,
            dispatchPipStateUseCase = dispatchPipStateUseCase,
            triggerPipActionUseCase = triggerPipActionUseCase,
            evaluatePipEligibilityUseCase = evaluatePipEligibilityUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPipSourceModelCalculations() {
        val defaultSource = PipSourceModel(tag = "test-1", priority = 10)
        assertEquals(16, defaultSource.aspectRatioWidth)
        assertEquals(9, defaultSource.aspectRatioHeight)
        assertTrue(defaultSource.aspectRatio > 1.7f)
        assertTrue(defaultSource.isEligible)

        val customRatio = PipSourceModel(tag = "test-2", priority = 5, aspectRatioWidth = 4, aspectRatioHeight = 3)
        assertEquals(4f / 3f, customRatio.aspectRatio, 0.01f)

        val unavailable = PipSourceModel(tag = "test-3", isAvailable = false, isAttachedToPip = false)
        assertFalse(unavailable.isEligible)

        val attachedWhileUnavailable = PipSourceModel(tag = "test-4", isAvailable = false, isAttachedToPip = true)
        assertTrue(attachedWhileUnavailable.isEligible)
    }

    @Test
    fun testPriorityArbitrationAndEligibility() = runTest(testDispatcher) {
        val lowPriority = PipSourceModel(tag = "video-player", priority = 10, isAvailable = true, needsMediaSession = true)
        val highPriority = PipSourceModel(tag = "voip-call", priority = 100, isAvailable = true, needsMediaSession = false)

        repository.registerSource(lowPriority)
        var session = repository.getSessionInfo()
        assertEquals("video-player", session.activeSource?.tag)
        assertTrue(session.isMediaSessionActive)
        assertTrue(repository.canEnterPip())

        // Register higher priority source
        repository.registerSource(highPriority)
        session = repository.getSessionInfo()
        assertEquals("voip-call", session.activeSource?.tag)
        assertFalse(session.isMediaSessionActive)

        // If high priority source becomes unavailable, fallback to low priority
        repository.updateSourceAvailability("voip-call", isAvailable = false)
        session = repository.getSessionInfo()
        assertEquals("video-player", session.activeSource?.tag)
        assertTrue(session.isMediaSessionActive)

        // If high priority is attached to PiP even when unavailable, it retains priority
        repository.updateSourceAttached("voip-call", isAttached = true)
        session = repository.getSessionInfo()
        assertEquals("voip-call", session.activeSource?.tag)

        // Unregister high priority source
        repository.unregisterSource("voip-call")
        session = repository.getSessionInfo()
        assertEquals("video-player", session.activeSource?.tag)

        // Unregister low priority source
        repository.unregisterSource("video-player")
        session = repository.getSessionInfo()
        assertNull(session.activeSource)
        assertFalse(session.hasContentForPip)
        assertFalse(repository.canEnterPip())
    }

    @Test
    fun testPipLifecycleTransitions() = runTest(testDispatcher) {
        var session = repository.getSessionInfo()
        assertEquals(PipState.IDLE, session.pipState)
        assertFalse(session.isInPip)

        repository.updatePipState(PipState.ENTERING)
        session = repository.getSessionInfo()
        assertEquals(PipState.ENTERING, session.pipState)

        repository.updatePipState(PipState.IN_PIP)
        session = repository.getSessionInfo()
        assertEquals(PipState.IN_PIP, session.pipState)
        assertTrue(session.isInPip)

        repository.updatePipState(PipState.STASHED)
        session = repository.getSessionInfo()
        assertEquals(PipState.STASHED, session.pipState)
        assertTrue(session.isInPip)

        repository.updatePipState(PipState.EXITING, byActivityStop = true)
        session = repository.getSessionInfo()
        assertEquals(PipState.EXITING, session.pipState)
        assertFalse(session.isInPip)

        repository.updatePipState(PipState.IDLE)
        session = repository.getSessionInfo()
        assertEquals(PipState.IDLE, session.pipState)
    }

    @Test
    fun testPipActionTrigger() = runTest(testDispatcher) {
        repository.triggerPipAction("video-player", 1)
        val session = repository.getSessionInfo()
        assertNotNull(session.lastAction)
        assertEquals("video-player", session.lastAction?.first)
        assertEquals(1, session.lastAction?.second)
    }

    @Test
    fun testPipViewModelEventProcessing() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val source = PipSourceModel(tag = "pip-stream", priority = 50, isAvailable = true, needsMediaSession = true)
        viewModel.onEvent(PipEvent.RegisterSource(source))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("pip-stream", state.activeSourceTag)
        assertEquals(50, state.activeSourcePriority)
        assertTrue(state.isMediaSessionActive)
        assertTrue(state.canEnterPip)
        assertEquals(1, state.registeredSourceCount)

        // Ratio change event
        viewModel.onEvent(PipEvent.SetSourceRatio("pip-stream", 4, 3))
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(4, state.activeSource?.aspectRatioWidth)
        assertEquals(3, state.activeSource?.aspectRatioHeight)

        // State transition event
        viewModel.onEvent(PipEvent.TransitionPipState(PipState.IN_PIP))
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(PipState.IN_PIP, state.pipState)
        assertTrue(state.isInPip)

        // Action trigger event
        viewModel.onEvent(PipEvent.TriggerAction("pip-stream", 42))
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("pip-stream", state.lastAction?.first)
        assertEquals(42, state.lastAction?.second)

        // Unregister event
        viewModel.onEvent(PipEvent.UnregisterSource("pip-stream"))
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertNull(state.activeSource)
        assertEquals(0, state.registeredSourceCount)
        assertFalse(state.canEnterPip)
    }
}
