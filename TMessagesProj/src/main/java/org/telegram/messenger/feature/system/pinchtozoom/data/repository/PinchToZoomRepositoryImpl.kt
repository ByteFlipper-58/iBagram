package org.telegram.messenger.feature.system.pinchtozoom.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.pinchtozoom.data.datasource.PinchToZoomLocalDataSource
import org.telegram.messenger.feature.system.pinchtozoom.data.datasource.PinchToZoomRemoteDataSource
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchZoomState
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class PinchToZoomRepositoryImpl(
    private val localDataSource: PinchToZoomLocalDataSource,
    private val remoteDataSource: PinchToZoomRemoteDataSource
) : PinchToZoomRepository {

    override fun observeState(): StateFlow<PinchZoomState> = localDataSource.observeState()

    override fun getState(): PinchZoomState = localDataSource.getState()

    override fun calculateScale(currentDistance: Float, startDistance: Float): Float {
        return localDataSource.calculateScale(currentDistance, startDistance)
    }

    override fun calculateTranslation(
        startCenterX: Float,
        startCenterY: Float,
        currentCenterX: Float,
        currentCenterY: Float,
        scale: Float
    ): Pair<Float, Float> {
        return localDataSource.calculateTranslation(startCenterX, startCenterY, currentCenterX, currentCenterY, scale)
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
        return localDataSource.calculateTransform(
            scale, finishProgress, parentOffsetX, parentOffsetY, pinchCenterX, pinchCenterY, translationX, translationY
        )
    }

    override fun calculateImageBounds(dimensions: PinchImageDimensions, scale: Float): PinchBoundsResult {
        return localDataSource.calculateImageBounds(dimensions, scale)
    }

    override fun evaluatePinchGesture(
        startDistance: Float,
        startCenterX: Float,
        startCenterY: Float,
        point1: PinchTouchPoint,
        point2: PinchTouchPoint,
        isInOverlay: Boolean
    ): PinchGestureDecision {
        return localDataSource.evaluatePinchGesture(startDistance, startCenterX, startCenterY, point1, point2, isInOverlay)
    }

    override fun startZoom(scale: Float) {
        localDataSource.startZoom(scale)
    }

    override fun updateZoom(scale: Float, translationX: Float, translationY: Float) {
        localDataSource.updateZoom(scale, translationX, translationY)
    }

    override fun finishZoom(progress: Float) {
        localDataSource.finishZoom(progress)
    }

    override fun reset() {
        localDataSource.reset()
    }
}
