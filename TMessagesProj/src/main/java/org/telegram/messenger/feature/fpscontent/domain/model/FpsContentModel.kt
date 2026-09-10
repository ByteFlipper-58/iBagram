package org.telegram.messenger.feature.fpscontent.domain.model

/**
 * Types of callbacks and invalidations managed by the 60fps arbitration engine.
 */
enum class FrameCallbackType {
    FRAME_TICK,
    RUNNABLE,
    VIEW_INVALIDATION,
    DRAWABLE_INVALIDATION,
    DRAWABLE_INVALIDATION_30FPS
}

/**
 * Configuration for an FPS callback group.
 * If targetFps % fps == 0, stride > 0 and callbacks fire every [stride] ticks of global counter.
 * Otherwise, stride == 0 and [intervalNs] accumulator is used to support any arbitrary rate.
 */
data class FpsGroupConfig(
    val fps: Int,
    val intervalNs: Long,
    val stride: Int
) {
    val isStrideBased: Boolean get() = stride > 0
}

/**
 * Information about a dispatched frame tick.
 */
data class FrameTick(
    val frameTimeNanos: Long,
    val counter: Long,
    val fps: Int,
    val isStrideMatch: Boolean
)

/**
 * Subscription record for a persistent or one-shot callback.
 */
data class FrameCallbackSubscription(
    val id: String,
    val fps: Int,
    val isOneShot: Boolean,
    val type: FrameCallbackType,
    val registeredAtNanos: Long
)

/**
 * Snapshot of current 60fps arbitrator state.
 */
data class FpsContentStats(
    val isRunning: Boolean = true,
    val targetFps: Int = 60,
    val activeGroupsCount: Int = 0,
    val totalSubscriptionsCount: Int = 0,
    val totalDispatchedFrames: Long = 0L,
    val pendingOneShotCount: Int = 0,
    val pendingDrawablesCount: Int = 0,
    val pendingDrawables30fpsCount: Int = 0,
    val pendingViewsCount: Int = 0,
    val currentVsyncAccumulatorNs: Long = 0L
)

/**
 * Mathematical calculations and rate limiting utilities for frame timing.
 */
object FpsTimingUtils {
    const val TARGET_FPS: Int = 60
    const val TARGET_FRAME_INTERVAL_NS: Long = 1_000_000_000L / TARGET_FPS // ~16_666_666 ns

    fun clampFps(fps: Int): Int {
        return fps.coerceIn(1, TARGET_FPS)
    }

    fun calculateIntervalNs(fps: Int): Long {
        val safeFps = clampFps(fps)
        return 1_000_000_000L / safeFps
    }

    fun calculateStride(targetFps: Int, fps: Int): Int {
        val safeFps = clampFps(fps)
        return if (targetFps % safeFps == 0) targetFps / safeFps else 0
    }

    /**
     * Determines whether a callback group should fire on the current frame tick.
     * @return Pair of (shouldFire, updatedAccumulatedNs)
     */
    fun shouldFire(
        counter: Long,
        stride: Int,
        currentAccumulatedNs: Long,
        intervalNs: Long,
        tickIntervalNs: Long = TARGET_FRAME_INTERVAL_NS
    ): Pair<Boolean, Long> {
        return if (stride > 0) {
            val fire = (counter % stride) == 0L
            Pair(fire, currentAccumulatedNs)
        } else {
            val newAcc = currentAccumulatedNs + tickIntervalNs
            if (newAcc >= intervalNs) {
                Pair(true, newAcc % intervalNs)
            } else {
                Pair(false, newAcc)
            }
        }
    }
}
