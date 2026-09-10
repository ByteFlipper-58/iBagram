package org.telegram.messenger.feature.system.pinchtozoom.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.system.pinchtozoom.data.mapper.PinchToZoomMapper
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchZoomState
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

/**
 * Thread-safe adapter implementation for PinchToZoom repository.
 */
class LegacyPinchToZoomRepository(
    initialState: PinchZoomState? = null
) : PinchToZoomRepository {

    private val _state = MutableStateFlow(initialState ?: PinchZoomState())
    private val state: StateFlow<PinchZoomState> = _state.asStateFlow()

    override fun observeState(): StateFlow<PinchZoomState> = state

    override fun getState(): PinchZoomState = _state.value

    override fun calculateScale(currentDistance: Float, startDistance: Float): Float {
        return PinchToZoomMapper.calculateScale(currentDistance, startDistance)
    }

    override fun calculateTranslation(
        startCenterX: Float,
        startCenterY: Float,
        currentCenterX: Float,
        currentCenterY: Float,
        scale: Float
    ): Pair<Float, Float> {
        return PinchToZoomMapper.calculateTranslation(
            startCenterX = startCenterX,
            startCenterY = startCenterY,
            currentCenterX = currentCenterX,
            currentCenterY = currentCenterY,
            scale = scale
        )
    }

    override fun calculateTransform(
        scale: Float,
        finishProgress: Float,
        parentOffsetX: Float,
        parentOffsetY: Float,
        pinchCenterX: Float,
        pinchCenterY: Float,
        translationX: Float,
        translationY: Float
    ): PinchTransform {
        return PinchToZoomMapper.calculateTransform(
            scale = scale,
            finishProgress = finishProgress,
            parentOffsetX = parentOffsetX,
            parentOffsetY = parentOffsetY,
            pinchCenterX = pinchCenterX,
            pinchCenterY = pinchCenterY,
            translationX = translationX,
            translationY = translationY
        )
    }

    override fun calculateImageBounds(
        dimensions: PinchImageDimensions,
        scale: Float
    ): PinchBoundsResult {
        return PinchToZoomMapper.calculateImageBounds(dimensions, scale)
    }

    override fun evaluatePinchGesture(
        startDistance: Float,
        startCenterX: Float,
        startCenterY: Float,
        point1: PinchTouchPoint,
        point2: PinchTouchPoint,
        isInOverlay: Boolean
    ): PinchGestureDecision {
        return PinchToZoomMapper.evaluatePinchGesture(
            startDistance = startDistance,
            startCenterX = startCenterX,
            startCenterY = startCenterY,
            point1 = point1,
            point2 = point2,
            isInOverlay = isInOverlay
        )
    }

    override fun startZoom(scale: Float) {
        _state.update {
            it.copy(
                isInOverlayMode = true,
                isInTouchMode = true,
                scale = scale,
                finishProgress = 1f
            )
        }
    }

    override fun updateZoom(scale: Float, translationX: Float, translationY: Float) {
        _state.update {
            it.copy(
                scale = scale,
                translationX = translationX,
                translationY = translationY
            )
        }
    }

    override fun finishZoom(progress: Float) {
        _state.update {
            val clampedProgress = progress.coerceIn(0f, 1f)
            it.copy(
                finishProgress = clampedProgress,
                isInTouchMode = clampedProgress > 0f,
                isInOverlayMode = clampedProgress > 0f
            )
        }
    }

    override fun reset() {
        _state.value = PinchZoomState()
    }
}
