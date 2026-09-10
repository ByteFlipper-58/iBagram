package org.telegram.messenger.feature.pinchtozoom.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTransform
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

class PinchToZoomViewModel(
    private val observeZoomStateUseCase: ObservePinchZoomStateUseCase,
    private val getZoomStateUseCase: GetPinchZoomStateUseCase,
    private val calculateScaleUseCase: CalculatePinchScaleUseCase,
    private val calculateTranslationUseCase: CalculatePinchTranslationUseCase,
    private val calculateTransformUseCase: CalculatePinchTransformUseCase,
    private val calculateImageBoundsUseCase: CalculatePinchImageBoundsUseCase,
    private val evaluatePinchGestureUseCase: EvaluatePinchGestureUseCase,
    private val startZoomUseCase: StartPinchZoomUseCase,
    private val updateZoomUseCase: UpdatePinchZoomUseCase,
    private val finishZoomUseCase: FinishPinchZoomUseCase,
    private val resetUseCase: ResetPinchZoomUseCase
) : ViewModel() {

    private var startDistance: Float = 0f
    private var startCenterX: Float = 0f
    private var startCenterY: Float = 0f

    private val _uiState = MutableStateFlow(
        PinchToZoomUiState(
            zoomState = getZoomStateUseCase(),
            isGestureActive = false
        )
    )
    val uiState: StateFlow<PinchToZoomUiState> = _uiState.asStateFlow()

    init {
        observeZoomStateUseCase()
            .onEach { state ->
                _uiState.update { it.copy(zoomState = state) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PinchToZoomEvent) {
        when (event) {
            is PinchToZoomEvent.StartGesture -> {
                startDistance = kotlin.math.hypot(event.point2.x - event.point1.x, event.point2.y - event.point1.y)
                startCenterX = (event.point1.x + event.point2.x) / 2f
                startCenterY = (event.point1.y + event.point2.y) / 2f
                _uiState.update { it.copy(isGestureActive = true) }
            }
            is PinchToZoomEvent.UpdateGesture -> {
                val decision = evaluatePinchGestureUseCase(
                    startDistance = startDistance,
                    startCenterX = startCenterX,
                    startCenterY = startCenterY,
                    point1 = event.point1,
                    point2 = event.point2,
                    isInOverlay = _uiState.value.zoomState.isInOverlayMode
                )
                if (decision.shouldStartZoom) {
                    startZoomUseCase(decision.scale)
                }
                if (_uiState.value.zoomState.isInOverlayMode || decision.shouldStartZoom) {
                    updateZoomUseCase(decision.scale, decision.translationX, decision.translationY)
                }
            }
            is PinchToZoomEvent.StartZoom -> {
                startZoomUseCase(event.scale)
            }
            is PinchToZoomEvent.UpdateZoom -> {
                updateZoomUseCase(event.scale, event.translationX, event.translationY)
            }
            is PinchToZoomEvent.FinishZoom -> {
                finishZoomUseCase(event.progress)
            }
            PinchToZoomEvent.EndGesture -> {
                _uiState.update { it.copy(isGestureActive = false) }
                finishZoomUseCase(0f)
            }
            PinchToZoomEvent.Reset -> {
                startDistance = 0f
                startCenterX = 0f
                startCenterY = 0f
                _uiState.update { it.copy(isGestureActive = false, currentTransform = null) }
                resetUseCase()
            }
        }
    }

    fun calculateTransform(
        parentOffsetX: Float,
        parentOffsetY: Float,
        pinchCenterX: Float,
        pinchCenterY: Float
    ): PinchTransform {
        val state = _uiState.value.zoomState
        val transform = calculateTransformUseCase(
            scale = state.scale,
            finishProgress = state.finishProgress,
            parentOffsetX = parentOffsetX,
            parentOffsetY = parentOffsetY,
            pinchCenterX = pinchCenterX,
            pinchCenterY = pinchCenterY,
            translationX = state.translationX,
            translationY = state.translationY
        )
        _uiState.update { it.copy(currentTransform = transform) }
        return transform
    }

    fun calculateImageBounds(dimensions: PinchImageDimensions): PinchBoundsResult {
        return calculateImageBoundsUseCase(dimensions, _uiState.value.zoomState.scale)
    }

    fun evaluateGesture(
        point1: PinchTouchPoint,
        point2: PinchTouchPoint
    ): PinchGestureDecision {
        return evaluatePinchGestureUseCase(
            startDistance = startDistance,
            startCenterX = startCenterX,
            startCenterY = startCenterY,
            point1 = point1,
            point2 = point2,
            isInOverlay = _uiState.value.zoomState.isInOverlayMode
        )
    }
}
