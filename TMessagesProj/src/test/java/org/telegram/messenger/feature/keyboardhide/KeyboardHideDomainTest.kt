package org.telegram.messenger.feature.keyboardhide

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
import org.telegram.messenger.feature.keyboardhide.data.repository.LegacyKeyboardHideRepository
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.keyboardhide.domain.usecase.CalculateKeyboardHideProgressUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EndKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EvaluateKeyboardDismissDecisionUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.FinishKeyboardHideDismissUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.GetKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ObserveKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ResetKeyboardHideUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.SetKeyboardHideEnabledUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.StartKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.UpdateKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.presentation.KeyboardHideEvent
import org.telegram.messenger.feature.keyboardhide.presentation.KeyboardHideViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardHideDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyKeyboardHideRepository

    private lateinit var calculateProgressUseCase: CalculateKeyboardHideProgressUseCase
    private lateinit var evaluateDismissDecisionUseCase: EvaluateKeyboardDismissDecisionUseCase
    private lateinit var observeStateUseCase: ObserveKeyboardHideStateUseCase
    private lateinit var getStateUseCase: GetKeyboardHideStateUseCase
    private lateinit var setEnabledUseCase: SetKeyboardHideEnabledUseCase
    private lateinit var startMovingUseCase: StartKeyboardHideMovingUseCase
    private lateinit var updateMovingUseCase: UpdateKeyboardHideMovingUseCase
    private lateinit var endMovingUseCase: EndKeyboardHideMovingUseCase
    private lateinit var finishDismissUseCase: FinishKeyboardHideDismissUseCase
    private lateinit var resetUseCase: ResetKeyboardHideUseCase

    private lateinit var viewModel: KeyboardHideViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyKeyboardHideRepository(initialEnabled = true)

        calculateProgressUseCase = CalculateKeyboardHideProgressUseCase(repository)
        evaluateDismissDecisionUseCase = EvaluateKeyboardDismissDecisionUseCase(repository)
        observeStateUseCase = ObserveKeyboardHideStateUseCase(repository)
        getStateUseCase = GetKeyboardHideStateUseCase(repository)
        setEnabledUseCase = SetKeyboardHideEnabledUseCase(repository)
        startMovingUseCase = StartKeyboardHideMovingUseCase(repository)
        updateMovingUseCase = UpdateKeyboardHideMovingUseCase(repository)
        endMovingUseCase = EndKeyboardHideMovingUseCase(repository)
        finishDismissUseCase = FinishKeyboardHideDismissUseCase(repository)
        resetUseCase = ResetKeyboardHideUseCase(repository)

        viewModel = KeyboardHideViewModel(
            calculateProgressUseCase = calculateProgressUseCase,
            evaluateDismissDecisionUseCase = evaluateDismissDecisionUseCase,
            observeStateUseCase = observeStateUseCase,
            getStateUseCase = getStateUseCase,
            setEnabledUseCase = setEnabledUseCase,
            startMovingUseCase = startMovingUseCase,
            updateMovingUseCase = updateMovingUseCase,
            endMovingUseCase = endMovingUseCase,
            finishDismissUseCase = finishDismissUseCase,
            resetUseCase = resetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculateProgress_shouldReturnZeros_whenKeyboardSizeIsNonPositive() {
        val spec = KeyboardDragSpec(
            fromY = 1000f,
            currentRawY = 1100f,
            keyboardSize = 0
        )
        val result = calculateProgressUseCase(spec)
        assertEquals(0f, result.rawProgress, 0.01f)
        assertEquals(0f, result.clampedProgress, 0.01f)
        assertEquals(0f, result.translationY, 0.01f)
    }

    @Test
    fun calculateProgress_shouldCalculateTranslationAndInset_withNavbarCompensation() {
        // Keyboard height 600, bottom navbar 50, pulled down 300px (halfway)
        val spec = KeyboardDragSpec(
            fromY = 1000f,
            currentRawY = 1300f,
            keyboardSize = 600,
            bottomNavBarSize = 50,
            isKeyboard = true
        )
        val result = calculateProgressUseCase(spec)
        assertEquals(0.5f, result.rawProgress, 0.01f)
        assertEquals(0.5f, result.clampedProgress, 0.01f)
        assertEquals(300, result.insetHeight)
        // translationY = maxOf(0, (1 - 0.5) * 600 - 50 - 1) = 300 - 51 = 249
        assertEquals(249f, result.translationY, 0.01f)
        assertEquals(0.5f, result.alpha, 0.01f)
    }

    @Test
    fun calculateProgress_shouldCalculateTranslation_forPopupWithoutNavbarCompensation() {
        // Emoji/sticker popup height 500, pulled down 100px (20%)
        val spec = KeyboardDragSpec(
            fromY = 1000f,
            currentRawY = 1100f,
            keyboardSize = 500,
            bottomNavBarSize = 50,
            isKeyboard = false
        )
        val result = calculateProgressUseCase(spec)
        assertEquals(0.2f, result.rawProgress, 0.01f)
        assertEquals(0.2f, result.clampedProgress, 0.01f)
        // translationY = (1 - 0.2) * 500 = 400
        assertEquals(400f, result.translationY, 0.01f)
    }

    @Test
    fun calculateProgress_shouldClampBetweenZeroAndOne() {
        // Negative drag (pulled upwards)
        val specUp = KeyboardDragSpec(
            fromY = 1000f,
            currentRawY = 900f,
            keyboardSize = 500
        )
        val resultUp = calculateProgressUseCase(specUp)
        assertEquals(-0.2f, resultUp.rawProgress, 0.01f)
        assertEquals(0f, resultUp.clampedProgress, 0.01f)

        // Excessive drag beyond keyboard bottom
        val specDown = KeyboardDragSpec(
            fromY = 1000f,
            currentRawY = 1800f,
            keyboardSize = 500
        )
        val resultDown = calculateProgressUseCase(specDown)
        assertEquals(1.6f, resultDown.rawProgress, 0.01f)
        assertEquals(1f, resultDown.clampedProgress, 0.01f)
    }

    @Test
    fun evaluateDismissDecision_shouldDetectDismissalThresholds() {
        // Large pull (> 0.8)
        val decisionDeep = evaluateDismissDecisionUseCase(0.85f, 0.80f, 0f)
        assertTrue(decisionDeep.shouldDismiss)
        assertEquals(1f, decisionDeep.targetProgress, 0.01f)

        // Moderate forward pull (> 0.15 and moving downwards)
        val decisionForward = evaluateDismissDecisionUseCase(0.25f, 0.20f, 0f)
        assertTrue(decisionForward.shouldDismiss)

        // Fast flick downwards
        val decisionFlick = evaluateDismissDecisionUseCase(0.10f, 0.05f, 1500f)
        assertTrue(decisionFlick.shouldDismiss)

        // Insufficient drag (< 0.15)
        val decisionSmall = evaluateDismissDecisionUseCase(0.10f, 0.05f, 0f)
        assertFalse(decisionSmall.shouldDismiss)
        assertEquals(0f, decisionSmall.targetProgress, 0.01f)

        // Moving backwards (dragging back up)
        val decisionReversing = evaluateDismissDecisionUseCase(0.30f, 0.40f, 0f)
        assertFalse(decisionReversing.shouldDismiss)
    }

    @Test
    fun repository_lifecycle_shouldUpdateStateFlowAndDisableScrolling() {
        repository.startMoving(keyboardSize = 600, bottomNavBarSize = 50, isKeyboard = true)
        var state = repository.getState()
        assertTrue(state.isMovingKeyboard)
        assertFalse(state.isEndingMovingKeyboard)
        assertTrue(state.disableScrolling)
        assertEquals(600, state.keyboardSize)

        repository.updateMoving(rawProgress = 0.4f, progress = 0.4f, translationY = 300f)
        state = repository.getState()
        assertEquals(0.4f, state.currentProgress, 0.01f)
        assertEquals(300f, state.translationY, 0.01f)

        repository.endMoving(shouldDismiss = true)
        state = repository.getState()
        assertFalse(state.isMovingKeyboard)
        assertTrue(state.isEndingMovingKeyboard)
        assertTrue(state.disableScrolling)

        repository.finishDismiss(dismissed = true)
        state = repository.getState()
        assertFalse(state.isMovingKeyboard)
        assertFalse(state.isEndingMovingKeyboard)
        assertFalse(state.disableScrolling)
        assertEquals(1f, state.currentProgress, 0.01f)

        repository.reset()
        state = repository.getState()
        assertEquals(0f, state.currentProgress, 0.01f)
    }

    @Test
    fun viewModel_shouldReactToMviEvents() = runTest {
        viewModel.onEvent(KeyboardHideEvent.StartDrag(keyboardSize = 500, bottomNavBarSize = 40, isKeyboard = true))
        advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertTrue(uiState.isDragging)
        assertTrue(uiState.hideState.isMovingKeyboard)

        viewModel.onEvent(KeyboardHideEvent.UpdateDrag(rawProgress = 0.5f, progress = 0.5f, translationY = 200f))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(0.5f, uiState.hideState.currentProgress, 0.01f)
        assertEquals(200f, uiState.hideState.translationY, 0.01f)

        viewModel.onEvent(KeyboardHideEvent.EndDrag(shouldDismiss = true))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertFalse(uiState.isDragging)
        assertTrue(uiState.hideState.isEndingMovingKeyboard)

        viewModel.onEvent(KeyboardHideEvent.FinishDismiss(dismissed = true))
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertFalse(uiState.hideState.isEndingMovingKeyboard)
        assertEquals(1f, uiState.hideState.currentProgress, 0.01f)

        viewModel.onEvent(KeyboardHideEvent.SetEnabled(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hideState.isEnabled)
    }
}
