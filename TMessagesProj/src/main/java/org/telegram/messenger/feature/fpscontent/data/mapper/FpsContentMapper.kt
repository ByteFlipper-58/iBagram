package org.telegram.messenger.feature.fpscontent.data.mapper

import org.telegram.messenger.feature.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.fpscontent.domain.model.FpsGroupConfig
import org.telegram.messenger.feature.fpscontent.domain.model.FpsTimingUtils
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackType
import org.telegram.messenger.feature.fpscontent.domain.model.FrameTick

object FpsContentMapper {

    fun toFpsGroupConfig(fps: Int, targetFps: Int = FpsTimingUtils.TARGET_FPS): FpsGroupConfig {
        val safeFps = FpsTimingUtils.clampFps(fps)
        return FpsGroupConfig(
            fps = safeFps,
            intervalNs = FpsTimingUtils.calculateIntervalNs(safeFps),
            stride = FpsTimingUtils.calculateStride(targetFps, safeFps)
        )
    }

    fun toSubscription(
        id: String,
        fps: Int,
        isOneShot: Boolean,
        type: FrameCallbackType,
        registeredAtNanos: Long
    ): FrameCallbackSubscription {
        return FrameCallbackSubscription(
            id = id,
            fps = FpsTimingUtils.clampFps(fps),
            isOneShot = isOneShot,
            type = type,
            registeredAtNanos = registeredAtNanos
        )
    }

    fun toFrameTick(
        frameTimeNanos: Long,
        counter: Long,
        fps: Int,
        isStrideMatch: Boolean
    ): FrameTick {
        return FrameTick(
            frameTimeNanos = frameTimeNanos,
            counter = counter,
            fps = fps,
            isStrideMatch = isStrideMatch
        )
    }

    fun formatSummary(stats: FpsContentStats): String {
        return "FpsArbitrator[activeGroups=${stats.activeGroupsCount}, subscriptions=${stats.totalSubscriptionsCount}, dispatchedFrames=${stats.totalDispatchedFrames}, pendingViews=${stats.pendingViewsCount}, pendingDrawables=${stats.pendingDrawablesCount + stats.pendingDrawables30fpsCount}]"
    }
}
