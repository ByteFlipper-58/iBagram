package org.telegram.messenger.feature.system.adjustpan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.adjustpan.data.repository.LegacyAdjustPanRepository
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.CalculatePanTransitionPlanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ComputePanProgressUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.GetAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ObserveAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ResetAdjustPanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.SetAdjustPanEnabledUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StartAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StopAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.UpdateAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.presentation.AdjustPanEvent
import org.telegram.messenger.feature.system.adjustpan.presentation.AdjustPanViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AdjustPanDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyAdjustPanRepository

    private lateinit var calculatePlanUseCase: CalculatePanTransitionPlanUseCase
    private lateinit var computePanProgressUseCase: ComputePanProgressUseCase
    private lateinit var observeAdjustPanStateUseCase: ObserveAdjustPanStateUseCase
    private lateinit var getAdjustPanStateUseCase: GetAdjustPanStateUseCase
    private lateinit var setAdjustPanEnabledUseCase: SetAdjustPanEnabledUseCase
    private lateinit var startAdjustPanTransitionUseCase: StartAdjustPanTransitionUseCase
    private lateinit var updateAdjustPanTransitionUseCase: UpdateAdjustPanTransitionUseCase
    private lateinit var stopAdjustPanTransitionUseCase: StopAdjustPanTransitionUseCase
    private lateinit var resetAdjustPanUseCase: ResetAdjustPanUseCase

    private lateinit var viewModel: AdjustPanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyAdjustPanRepository()

        calculatePlanUseCase = CalculatePanTransitionPlanUseCase(repository)
        computePanProgressUseCase = ComputePanProgressUseCase(repository)
        observeAdjustPanStateUseCase = ObserveAdjustPanStateUseCase(repository)
        getAdjustPanStateUseCase = GetAdjustPanStateUseCase(repository)
        setAdjustPanEnabledUseCase = SetAdjustPanEnabledUseCase(repository)
        startAdjustPanTransitionUseCase = StartAdjustPanTransitionUseCase(repository)
        updateAdjustPanTransitionUseCase = UpdateAdjustPanTransitionUseCase(repository)
        stopAdjustPanTransitionUseCase = StopAdjustPanTransitionUseCase(repository)
        resetAdjustPanUseCase = ResetAdjustPanUseCase(repository)

        viewModel = AdjustPanViewModel(
            calculatePlanUseCase = calculatePlanUseCase,
            observeStateUseCase = observeAdjustPanStateUseCase,
            getAdjustPanStateUseCase = getAdjustPanStateUseCase,
            setEnabledUseCase = setAdjustPanEnabledUseCase,
            startTransitionUseCase = startAdjustPanTransitionUseCase,
            updateTransitionUseCase = updateAdjustPanTransitionUseCase,
            stopTransitionUseCase = stopAdjustPanTransitionUseCase,
            resetUseCase = resetAdjustPanUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculatePlan_shouldReturnNoAnimation_whenPreviousHeightInvalidOrDeltaBelowThreshold() {
        val specInitial = PanCalculationSpec(
            previousHeight = -1,
            contentHeight = 1000,
            thresholdPx = 50
        )
        val planInitial = calculatePlanUseCase(specInitial)
        assertFalse(planInitial.shouldAnimate)

        val specSmallDelta = PanCalculationSpec(
            previousHeight = 1000,
            contentHeight = 1020,
            thresholdPx = 50
        )
        val planSmallDelta = calculatePlanUseCase(specSmallDelta)
        assertFalse(planSmallDelta.shouldAnimate)
    }

    @Test
    fun calculatePlan_shouldReturnNoAnimation_whenHeightAnimationDisabled() {
        val specDisabled = PanCalculationSpec(
            previousHeight = 1200,
            contentHeight = 800,
            thresholdPx = 50,
            isHeightAnimationEnabled = false
        )
        val planDisabled = calculatePlanUseCase(specDisabled)
        assertFalse(planDisabled.shouldAnimate)
    }

    @Test
    fun calculatePlan_shouldCalculateProperTrajectory_whenKeyboardAppears() {
        // Height shrinks from 1200 to 800 (keyboard 400px appears)
        val spec = PanCalculationSpec(
            previousHeight = 1200,
            contentHeight = 800,
            previousStartOffset = 20,
            startOffset = 0,
            contentViewBottom = 1200,
            additionalContentHeight = 50,
            bottomTabsHeight = 60,
            thresholdPx = 50,
            isHeightAnimationEnabled = true
        )

        val plan = calculatePlanUseCase(spec)
        assertTrue(plan.shouldAnimate)
        assertTrue(plan.isKeyboardVisible)
        assertTrue(plan.showingKeyboard)
        assertEquals(400f, plan.keyboardSize, 0.01f)
        assertEquals(-400f, plan.fromY, 0.01f)
        assertEquals(-20f, plan.toY, 0.01f)
        assertFalse(plan.inverse)
        // targetHeight = maxOf(1200, 800 + 50 + 60 = 910) -> 1200
        assertEquals(1200, plan.targetHeight)
    }

    @Test
    fun calculatePlan_shouldCalculateProperTrajectory_whenKeyboardHides() {
        // Height expands from 800 to 1200 (keyboard hides)
        val spec = PanCalculationSpec(
            previousHeight = 800,
            contentHeight = 1200,
            previousStartOffset = 0,
            startOffset = 30,
            contentViewBottom = 1200,
            additionalContentHeight = 0,
            bottomTabsHeight = 50,
            thresholdPx = 50,
            isHeightAnimationEnabled = true
        )

        val plan = calculatePlanUseCase(spec)
        assertTrue(plan.shouldAnimate)
        assertFalse(plan.isKeyboardVisible)
        assertFalse(plan.showingKeyboard)
        assertEquals(400f, plan.keyboardSize, 0.01f)
        // dy = 400 - startOffset (30) = 370 -> fromY = -370
        assertEquals(-370f, plan.fromY, 0.01f)
        assertEquals(-50f, plan.toY, 0.01f)
        assertTrue(plan.inverse)
        assertEquals(1250, plan.targetHeight)
    }

    @Test
    fun computeProgress_shouldCorrectlyInterpolateTrajectory() {
        val planForward = PanTransitionPlan(
            shouldAnimate = true,
            isKeyboardVisible = true,
            showingKeyboard = true,
            keyboardSize = 400f,
            targetHeight = 1200,
            fromY = -400f,
            toY = 0f,
            inverse = false
        )

        // When inverse = false: progress 0 -> y = toY (0f), progress 1 -> y = fromY (-400f)
        val atZero = computePanProgressUseCase(planForward, 0f)
        assertEquals(0f, atZero.translationY, 0.01f)
        val atHalf = computePanProgressUseCase(planForward, 0.5f)
        assertEquals(-200f, atHalf.translationY, 0.01f)
        val atOne = computePanProgressUseCase(planForward, 1f)
        assertEquals(-400f, atOne.translationY, 0.01f)

        // Inverse trajectory (hiding keyboard): progress 0 -> y = fromY (-400f), progress 1 -> y = toY (0f)
        val planInverse = PanTransitionPlan(
            shouldAnimate = true,
            isKeyboardVisible = false,
            showingKeyboard = false,
            keyboardSize = 400f,
            targetHeight = 1200,
            fromY = -400f,
            toY = 0f,
            inverse = true
        )

        val invZero = computePanProgressUseCase(planInverse, 0f)
        assertEquals(-400f, invZero.translationY, 0.01f)
        val invHalf = computePanProgressUseCase(planInverse, 0.5f)
        assertEquals(-200f, invHalf.translationY, 0.01f)
        val invOne = computePanProgressUseCase(planInverse, 1f)
        assertEquals(0f, invOne.translationY, 0.01f)
    }

    @Test
    fun repository_transitionLifecycle_shouldUpdateStateFlow() {
        val spec = PanCalculationSpec(
            previousHeight = 1200,
            contentHeight = 800,
            thresholdPx = 50
        )
        val plan = repository.calculatePlan(spec)
        assertTrue(plan.shouldAnimate)

        repository.startTransition(plan)
        var state = repository.getState()
        assertTrue(state.isAnimationInProgress)
        assertTrue(state.isShowingKeyboard)
        assertEquals(400f, state.keyboardSize, 0.01f)
        assertEquals(0f, state.currentTranslationY, 0.01f)

        repository.updateTransition(0.5f)
        state = repository.getState()
        assertEquals(0.5f, state.currentProgress, 0.01f)
        assertEquals(-200f, state.currentTranslationY, 0.01f)

        repository.updateTransition(1.0f)
        state = repository.getState()
        assertEquals(1.0f, state.currentProgress, 0.01f)
        assertEquals(-400f, state.currentTranslationY, 0.01f)

        repository.stopTransition(1f, isKeyboardVisible = true)
        state = repository.getState()
        assertFalse(state.isAnimationInProgress)
        assertEquals(0f, state.currentTranslationY, 0.01f)

        repository.reset()
        state = repository.getState()
        assertFalse(state.isAnimationInProgress)
        assertEquals(-1, state.targetHeight)
    }

    @Test
    fun viewModel_shouldReactToMviEvents() = runTest {
        val spec = PanCalculationSpec(
            previousHeight = 1000,
            contentHeight = 600,
            thresholdPx = 50
        )

        viewModel.onEvent(AdjustPanEvent.PrepareTransition(spec))
        advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertTrue(uiState.activePlan.shouldAnimate)
        assertTrue(uiState.transitionState.isAnimationInProgress)

        viewModel.onEvent(AdjustPanEvent.UpdateProgress(0.75f))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(0.75f, uiState.transitionState.currentProgress, 0.01f)

        viewModel.onEvent(AdjustPanEvent.CompleteTransition(1f, isKeyboardVisible = true))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertFalse(uiState.transitionState.isAnimationInProgress)
        assertEquals(PanTransitionPlan.NO_ANIMATION, uiState.activePlan)

        viewModel.onEvent(AdjustPanEvent.SetEnabled(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.transitionState.isEnabled)
    }
}
