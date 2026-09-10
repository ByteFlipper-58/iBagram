package org.telegram.messenger.feature.pinchtozoom

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.pinchtozoom.data.mapper.PinchToZoomMapper
import org.telegram.messenger.feature.pinchtozoom.data.repository.LegacyPinchToZoomRepository
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchImageBoundsUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchScaleUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchTransformUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchTranslationUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.EvaluatePinchGestureUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.FinishPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.GetPinchZoomStateUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.ObservePinchZoomStateUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.ResetPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.StartPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.UpdatePinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.presentation.PinchToZoomEvent
import org.telegram.messenger.feature.pinchtozoom.presentation.PinchToZoomViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PinchToZoomDomainTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LegacyPinchToZoomRepository

    private lateinit var observeZoomStateUseCase: ObservePinchZoomStateUseCase
    private lateinit var getZoomStateUseCase: GetPinchZoomStateUseCase
    private lateinit var calculateScaleUseCase: CalculatePinchScaleUseCase
    private lateinit var calculateTranslationUseCase: CalculatePinchTranslationUseCase
    private lateinit var calculateTransformUseCase: CalculatePinchTransformUseCase
    private lateinit var calculateImageBoundsUseCase: CalculatePinchImageBoundsUseCase
    private lateinit var evaluatePinchGestureUseCase: EvaluatePinchGestureUseCase
    private lateinit var startZoomUseCase: StartPinchZoomUseCase
    private lateinit var updateZoomUseCase: UpdatePinchZoomUseCase
    private lateinit var finishZoomUseCase: FinishPinchZoomUseCase
    private lateinit var resetUseCase: ResetPinchZoomUseCase

    private lateinit var viewModel: PinchToZoomViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyPinchToZoomRepository()

        observeZoomStateUseCase = ObservePinchZoomStateUseCase(repository)
        getZoomStateUseCase = GetPinchZoomStateUseCase(repository)
        calculateScaleUseCase = CalculatePinchScaleUseCase(repository)
        calculateTranslationUseCase = CalculatePinchTranslationUseCase(repository)
        calculateTransformUseCase = CalculatePinchTransformUseCase(repository)
        calculateImageBoundsUseCase = CalculatePinchImageBoundsUseCase(repository)
        evaluatePinchGestureUseCase = EvaluatePinchGestureUseCase(repository)
        startZoomUseCase = StartPinchZoomUseCase(repository)
        updateZoomUseCase = UpdatePinchZoomUseCase(repository)
        finishZoomUseCase = FinishPinchZoomUseCase(repository)
        resetUseCase = ResetPinchZoomUseCase(repository)

        viewModel = PinchToZoomViewModel(
            observeZoomStateUseCase = observeZoomStateUseCase,
            getZoomStateUseCase = getZoomStateUseCase,
            calculateScaleUseCase = calculateScaleUseCase,
            calculateTranslationUseCase = calculateTranslationUseCase,
            calculateTransformUseCase = calculateTransformUseCase,
            calculateImageBoundsUseCase = calculateImageBoundsUseCase,
            evaluatePinchGestureUseCase = evaluatePinchGestureUseCase,
            startZoomUseCase = startZoomUseCase,
            updateZoomUseCase = updateZoomUseCase,
            finishZoomUseCase = finishZoomUseCase,
            resetUseCase = resetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapper_calculateDistanceAndScale_shouldComputeCorrectly() {
        // (0,0) to (300, 400) -> distance = 500
        val dist = PinchToZoomMapper.calculateDistance(0f, 0f, 300f, 400f)
        assertEquals(500f, dist, 0.01f)

        // Scale: 750 / 500 = 1.5
        val scale = PinchToZoomMapper.calculateScale(750f, 500f)
        assertEquals(1.5f, scale, 0.01f)

        // Non-positive start distance
        val fallbackScale = PinchToZoomMapper.calculateScale(100f, 0f)
        assertEquals(1.0f, fallbackScale, 0.01f)
    }

    @Test
    fun mapper_calculateTranslation_shouldCompensateFocalShiftDividedByScale() {
        // Start center (100, 100), current center (150, 120), scale = 2.0
        // moveDx = 100 - 150 = -50 -> transX = -(-50) / 2 = 25
        // moveDy = 100 - 120 = -20 -> transY = -(-20) / 2 = 10
        val (transX, transY) = PinchToZoomMapper.calculateTranslation(
            startCenterX = 100f,
            startCenterY = 100f,
            currentCenterX = 150f,
            currentCenterY = 120f,
            scale = 2.0f
        )
        assertEquals(25f, transX, 0.01f)
        assertEquals(10f, transY, 0.01f)
    }

    @Test
    fun mapper_calculateTransform_shouldInterpolateScaleAndTranslationWithFinishProgress() {
        // Active zoom: scale = 2.5, finishProgress = 1f
        val transformActive = PinchToZoomMapper.calculateTransform(
            scale = 2.5f,
            finishProgress = 1.0f,
            parentOffsetX = 10f,
            parentOffsetY = 20f,
            pinchCenterX = 100f,
            pinchCenterY = 150f,
            translationX = 30f,
            translationY = 40f
        )
        assertEquals(2.5f, transformActive.scale, 0.01f)
        assertEquals(110f, transformActive.pivotX, 0.01f)
        assertEquals(170f, transformActive.pivotY, 0.01f)
        assertEquals(40f, transformActive.translationX, 0.01f)
        assertEquals(60f, transformActive.translationY, 0.01f)

        // Finished zoom: finishProgress = 0f -> scale = 1.0, translation collapses to parentOffset
        val transformFinished = PinchToZoomMapper.calculateTransform(
            scale = 2.5f,
            finishProgress = 0.0f,
            parentOffsetX = 10f,
            parentOffsetY = 20f,
            pinchCenterX = 100f,
            pinchCenterY = 150f,
            translationX = 30f,
            translationY = 40f
        )
        assertEquals(1.0f, transformFinished.scale, 0.01f)
        assertEquals(10f, transformFinished.translationX, 0.01f)
        assertEquals(20f, transformFinished.translationY, 0.01f)
    }

    @Test
    fun mapper_calculateImageBounds_shouldInterpolateFullViewPaddingBetween1And1Point4() {
        val dims = PinchImageDimensions(
            imageX = 50f,
            imageY = 50f,
            imageWidth = 200f,
            imageHeight = 100f,
            fullImageWidth = 300f,
            fullImageHeight = 200f
        )
        // horizontalPadding = (300 - 200) / 2 = 50, verticalPadding = (200 - 100) / 2 = 50

        // scale < 1.0 -> progress = 0
        val bounds1 = PinchToZoomMapper.calculateImageBounds(dims, 0.9f)
        assertEquals(0f, bounds1.progress, 0.01f)
        assertEquals(50f, bounds1.x, 0.01f)
        assertEquals(50f, bounds1.y, 0.01f)
        assertEquals(200f, bounds1.width, 0.01f)
        assertEquals(100f, bounds1.height, 0.01f)

        // scale = 1.2 -> progress = (1.2 - 1.0) / 0.4 = 0.5
        val boundsHalf = PinchToZoomMapper.calculateImageBounds(dims, 1.2f)
        assertEquals(0.5f, boundsHalf.progress, 0.01f)
        assertEquals(25f, boundsHalf.x, 0.01f) // 50 - 50 * 0.5
        assertEquals(25f, boundsHalf.y, 0.01f)
        assertEquals(250f, boundsHalf.width, 0.01f) // 200 + 50 * 0.5 * 2
        assertEquals(150f, boundsHalf.height, 0.01f)

        // scale >= 1.4 -> progress = 1.0
        val boundsFull = PinchToZoomMapper.calculateImageBounds(dims, 1.5f)
        assertEquals(1.0f, boundsFull.progress, 0.01f)
        assertEquals(0f, boundsFull.x, 0.01f)
        assertEquals(0f, boundsFull.y, 0.01f)
        assertEquals(300f, boundsFull.width, 0.01f)
        assertEquals(200f, boundsFull.height, 0.01f)
    }

    @Test
    fun mapper_evaluatePinchGesture_shouldTriggerStartZoom_whenThresholdExceeded() {
        val startDist = 100f
        val p1 = PinchTouchPoint(0f, 0f)

        // Under threshold: distance = 100.2 (scale = 1.002 <= 1.005)
        val p2Small = PinchTouchPoint(100.2f, 0f)
        val decisionSmall = PinchToZoomMapper.evaluatePinchGesture(
            startDistance = startDist,
            startCenterX = 50f,
            startCenterY = 0f,
            point1 = p1,
            point2 = p2Small,
            isInOverlay = false
        )
        assertFalse(decisionSmall.shouldStartZoom)

        // Beyond threshold: distance = 110 (scale = 1.1 > 1.005)
        val p2Large = PinchTouchPoint(110f, 0f)
        val decisionLarge = PinchToZoomMapper.evaluatePinchGesture(
            startDistance = startDist,
            startCenterX = 50f,
            startCenterY = 0f,
            point1 = p1,
            point2 = p2Large,
            isInOverlay = false
        )
        assertTrue(decisionLarge.shouldStartZoom)
        assertEquals(1.1f, decisionLarge.scale, 0.01f)

        // If already in overlay, should not trigger start again
        val decisionInOverlay = PinchToZoomMapper.evaluatePinchGesture(
            startDistance = startDist,
            startCenterX = 50f,
            startCenterY = 0f,
            point1 = p1,
            point2 = p2Large,
            isInOverlay = true
        )
        assertFalse(decisionInOverlay.shouldStartZoom)
    }

    @Test
    fun repository_lifecycle_shouldUpdateStateFlowAccurately() {
        var state = repository.getState()
        assertFalse(state.isInOverlayMode)
        assertFalse(state.isInTouchMode)
        assertEquals(1.0f, state.scale, 0.01f)

        repository.startZoom(scale = 1.2f)
        state = repository.getState()
        assertTrue(state.isInOverlayMode)
        assertTrue(state.isInTouchMode)
        assertEquals(1.2f, state.scale, 0.01f)
        assertEquals(1.0f, state.finishProgress, 0.01f)

        repository.updateZoom(scale = 1.8f, translationX = 15f, translationY = -25f)
        state = repository.getState()
        assertEquals(1.8f, state.scale, 0.01f)
        assertEquals(15f, state.translationX, 0.01f)
        assertEquals(-25f, state.translationY, 0.01f)

        repository.finishZoom(progress = 0.4f)
        state = repository.getState()
        assertEquals(0.4f, state.finishProgress, 0.01f)
        assertTrue(state.isInOverlayMode)

        repository.finishZoom(progress = 0f)
        state = repository.getState()
        assertFalse(state.isInOverlayMode)
        assertFalse(state.isInTouchMode)

        repository.reset()
        state = repository.getState()
        assertEquals(1.0f, state.scale, 0.01f)
        assertEquals(0f, state.translationX, 0.01f)
    }

    @Test
    fun viewModel_shouldHandleMviEventsAndComputeTransforms() = runTest {
        val p1 = PinchTouchPoint(100f, 100f, 0)
        val p2 = PinchTouchPoint(200f, 100f, 1) // distance = 100

        viewModel.onEvent(PinchToZoomEvent.StartGesture(p1, p2))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGestureActive)

        // Move fingers apart: distance = 150 (scale = 1.5)
        val p2Expanded = PinchTouchPoint(250f, 100f, 1)
        viewModel.onEvent(PinchToZoomEvent.UpdateGesture(p1, p2Expanded))
        advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertTrue(uiState.zoomState.isInOverlayMode)
        assertEquals(1.5f, uiState.zoomState.scale, 0.01f)

        // Calculate transform
        val transform = viewModel.calculateTransform(
            parentOffsetX = 20f,
            parentOffsetY = 30f,
            pinchCenterX = 150f,
            pinchCenterY = 100f
        )
        assertEquals(1.5f, transform.scale, 0.01f)
        assertNotNull(viewModel.uiState.value.currentTransform)

        // End gesture
        viewModel.onEvent(PinchToZoomEvent.EndGesture)
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertFalse(uiState.isGestureActive)
        assertFalse(uiState.zoomState.isInOverlayMode)

        // Reset
        viewModel.onEvent(PinchToZoomEvent.Reset)
        advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertNull(uiState.currentTransform)
        assertEquals(1.0f, uiState.zoomState.scale, 0.01f)
    }
}
