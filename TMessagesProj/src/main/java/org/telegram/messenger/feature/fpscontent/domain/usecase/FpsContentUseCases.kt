package org.telegram.messenger.feature.fpscontent.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.fpscontent.domain.model.FpsGroupConfig
import org.telegram.messenger.feature.fpscontent.domain.model.FpsTimingUtils
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.fpscontent.domain.model.FrameTick
import org.telegram.messenger.feature.fpscontent.domain.repository.FpsContentRepository

/**
 * Registers a frame timestamp callback at desired FPS.
 */
class RegisterFrameCallbackUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(
        fps: Int = 60,
        isOneShot: Boolean = false,
        onFrame: (Long) -> Unit
    ): String {
        return repository.addFrameCallback(fps, isOneShot, onFrame)
    }
}

/**
 * Registers a Runnable callback at desired FPS.
 */
class RegisterRunnableCallbackUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(
        fps: Int = 60,
        isOneShot: Boolean = false,
        action: () -> Unit
    ): String {
        return repository.addRunnableCallback(fps, isOneShot, action)
    }
}

/**
 * Unregisters a previously registered callback.
 */
class UnregisterCallbackUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(subscriptionId: String): Boolean {
        return repository.removeCallback(subscriptionId)
    }
}

/**
 * Requests invalidation for a view ID on the next 60fps tick.
 */
class RequestViewInvalidationUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(viewId: String): Boolean {
        return repository.postInvalidateView(viewId)
    }
}

/**
 * Requests invalidation for a drawable ID on the next 60fps or 30fps tick.
 */
class RequestDrawableInvalidationUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(drawableId: String, fps: Int = 60): Boolean {
        return repository.postInvalidateDrawable(drawableId, fps)
    }
}

/**
 * Feeds a vsync timestamp into the arbitration engine and triggers frame dispatches.
 */
class DispatchVsyncTickUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(frameTimeNanos: Long): List<FrameTick> {
        return repository.dispatchVsync(frameTimeNanos)
    }
}

/**
 * Pure calculation of FPS stride and interval nanoseconds.
 */
class CalculateFpsTimingUseCase {
    operator fun invoke(fps: Int, targetFps: Int = FpsTimingUtils.TARGET_FPS): FpsGroupConfig {
        val clampedFps = FpsTimingUtils.clampFps(fps)
        val intervalNs = FpsTimingUtils.calculateIntervalNs(clampedFps)
        val stride = FpsTimingUtils.calculateStride(targetFps, clampedFps)
        return FpsGroupConfig(
            fps = clampedFps,
            intervalNs = intervalNs,
            stride = stride
        )
    }
}

/**
 * Retrieves a snapshot of arbitrator statistics.
 */
class GetFpsContentStatsUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(): FpsContentStats {
        return repository.getStats()
    }
}

/**
 * Retrieves all active callback subscriptions.
 */
class GetFpsSubscriptionsUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(): List<FrameCallbackSubscription> {
        return repository.getSubscriptions()
    }
}

/**
 * Observes real-time statistics stream.
 */
class ObserveFpsContentStatsUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(): StateFlow<FpsContentStats> {
        return repository.observeStats()
    }
}

/**
 * Observes frame ticks stream.
 */
class ObserveFpsTicksUseCase(private val repository: FpsContentRepository) {
    operator fun invoke(): Flow<FrameTick> {
        return repository.observeTicks()
    }
}

/**
 * Resets the 60fps arbitration engine state.
 */
class ResetFpsContentUseCase(private val repository: FpsContentRepository) {
    operator fun invoke() {
        repository.reset()
    }
}
