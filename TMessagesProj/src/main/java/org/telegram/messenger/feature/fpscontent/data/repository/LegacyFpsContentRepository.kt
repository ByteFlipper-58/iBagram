package org.telegram.messenger.feature.fpscontent.data.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.fpscontent.data.mapper.FpsContentMapper
import org.telegram.messenger.feature.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.fpscontent.domain.model.FpsTimingUtils
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackType
import org.telegram.messenger.feature.fpscontent.domain.model.FrameTick
import org.telegram.messenger.feature.fpscontent.domain.repository.FpsContentRepository
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe implementation of [FpsContentRepository] isolating the logic of
 * [org.telegram.messenger.utils.Choreographer60FpsContent].
 *
 * Provides frame rate arbitration, stride groups for divisors of 60 FPS,
 * accumulator groups for arbitrary rates, one-shot actions, and view/drawable invalidations.
 */
class LegacyFpsContentRepository : FpsContentRepository {

    private val lock = Any()
    private val idGenerator = AtomicLong(0L)

    private var counter: Long = 0L
    private var lastVsyncNs: Long = 0L
    private var accumulatedVsyncNs: Long = 0L
    private var totalDispatchedFrames: Long = 0L

    private val oneShotCallbacks = LinkedHashMap<String, (Long) -> Unit>()
    private val groups = HashMap<Int, CallbackGroupHolder>()

    private val viewsToInvalidate = LinkedHashSet<String>()
    private val drawablesToInvalidate = LinkedHashSet<String>()
    private val drawablesToInvalidate30fps = LinkedHashSet<String>()
    private val allSubscriptions = LinkedHashMap<String, FrameCallbackSubscription>()

    private val _stats = MutableStateFlow(FpsContentStats())
    private val _ticksFlow = MutableSharedFlow<FrameTick>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private class CallbackGroupHolder(
        val fps: Int,
        val intervalNs: Long,
        val stride: Int
    ) {
        var accumulatedNs: Long = 0L
        val callbacks = LinkedHashMap<String, (Long) -> Unit>()
        val runnables = LinkedHashMap<String, () -> Unit>()
        val runnablesOnce = LinkedHashMap<String, () -> Unit>()

        val isEmpty: Boolean
            get() = callbacks.isEmpty() && runnables.isEmpty() && runnablesOnce.isEmpty()
    }

    override fun addFrameCallback(
        fps: Int,
        isOneShot: Boolean,
        onFrame: (Long) -> Unit
    ): String = synchronized(lock) {
        val safeFps = FpsTimingUtils.clampFps(fps)
        val id = "frame_cb_${idGenerator.incrementAndGet()}"
        val now = System.nanoTime()

        if (isOneShot && safeFps == FpsTimingUtils.TARGET_FPS) {
            oneShotCallbacks[id] = onFrame
        } else {
            val group = getOrCreateGroup(safeFps)
            group.callbacks[id] = onFrame
        }

        val sub = FpsContentMapper.toSubscription(
            id = id,
            fps = safeFps,
            isOneShot = isOneShot,
            type = FrameCallbackType.FRAME_TICK,
            registeredAtNanos = now
        )
        allSubscriptions[id] = sub
        updateStatsLocked()
        id
    }

    override fun addRunnableCallback(
        fps: Int,
        isOneShot: Boolean,
        action: () -> Unit
    ): String = synchronized(lock) {
        val safeFps = FpsTimingUtils.clampFps(fps)
        val id = "runnable_cb_${idGenerator.incrementAndGet()}"
        val now = System.nanoTime()
        val group = getOrCreateGroup(safeFps)

        if (isOneShot) {
            group.runnablesOnce[id] = action
        } else {
            group.runnables[id] = action
        }

        val sub = FpsContentMapper.toSubscription(
            id = id,
            fps = safeFps,
            isOneShot = isOneShot,
            type = FrameCallbackType.RUNNABLE,
            registeredAtNanos = now
        )
        allSubscriptions[id] = sub
        updateStatsLocked()
        id
    }

    override fun removeCallback(subscriptionId: String): Boolean = synchronized(lock) {
        var removed = oneShotCallbacks.remove(subscriptionId) != null
        if (!removed) {
            for (group in groups.values) {
                if (group.callbacks.remove(subscriptionId) != null ||
                    group.runnables.remove(subscriptionId) != null ||
                    group.runnablesOnce.remove(subscriptionId) != null
                ) {
                    removed = true
                    break
                }
            }
        }
        if (removed) {
            allSubscriptions.remove(subscriptionId)
            cleanEmptyGroupsLocked()
            updateStatsLocked()
        }
        removed
    }

    override fun postInvalidateView(viewId: String): Boolean = synchronized(lock) {
        val added = viewsToInvalidate.add(viewId)
        if (added) updateStatsLocked()
        added
    }

    override fun postInvalidateDrawable(drawableId: String, fps: Int): Boolean = synchronized(lock) {
        val added = if (fps <= 30) {
            drawablesToInvalidate30fps.add(drawableId)
        } else {
            drawablesToInvalidate.add(drawableId)
        }
        if (added) updateStatsLocked()
        added
    }

    override fun dispatchVsync(frameTimeNanos: Long): List<FrameTick> {
        val firedTicks = mutableListOf<FrameTick>()
        val actionsToRun = mutableListOf<() -> Unit>()

        synchronized(lock) {
            if (lastVsyncNs == 0L) {
                lastVsyncNs = frameTimeNanos
                return emptyList()
            }

            accumulatedVsyncNs += (frameTimeNanos - lastVsyncNs)
            lastVsyncNs = frameTimeNanos

            if (accumulatedVsyncNs >= FpsTimingUtils.TARGET_FRAME_INTERVAL_NS) {
                accumulatedVsyncNs %= FpsTimingUtils.TARGET_FRAME_INTERVAL_NS

                // 1. Process groups
                for (group in groups.values) {
                    val (shouldFire, updatedAcc) = FpsTimingUtils.shouldFire(
                        counter = counter,
                        stride = group.stride,
                        currentAccumulatedNs = group.accumulatedNs,
                        intervalNs = group.intervalNs
                    )
                    group.accumulatedNs = updatedAcc

                    if (shouldFire) {
                        val tick = FpsContentMapper.toFrameTick(
                            frameTimeNanos = frameTimeNanos,
                            counter = counter,
                            fps = group.fps,
                            isStrideMatch = group.stride > 0
                        )
                        firedTicks.add(tick)

                        // Run one-shot runnables
                        val onceSnapshot = ArrayList(group.runnablesOnce.values)
                        val onceIds = ArrayList(group.runnablesOnce.keys)
                        group.runnablesOnce.clear()
                        for (id in onceIds) {
                            allSubscriptions.remove(id)
                        }
                        actionsToRun.add {
                            for (action in onceSnapshot) action()
                        }

                        // Run persistent callbacks
                        val cbsSnapshot = ArrayList(group.callbacks.values)
                        actionsToRun.add {
                            for (cb in cbsSnapshot) cb(frameTimeNanos)
                        }

                        // Run persistent runnables
                        val runsSnapshot = ArrayList(group.runnables.values)
                        actionsToRun.add {
                            for (action in runsSnapshot) action()
                        }
                    }
                }

                // 2. Process one-shot callbacks
                if (oneShotCallbacks.isNotEmpty()) {
                    val onesSnapshot = ArrayList(oneShotCallbacks.values)
                    val oneIds = ArrayList(oneShotCallbacks.keys)
                    oneShotCallbacks.clear()
                    for (id in oneIds) {
                        allSubscriptions.remove(id)
                    }
                    actionsToRun.add {
                        for (cb in onesSnapshot) cb(frameTimeNanos)
                    }
                }

                // 3. Clear invalidations
                viewsToInvalidate.clear()
                drawablesToInvalidate.clear()
                if (counter % 2L == 0L) {
                    drawablesToInvalidate30fps.clear()
                }

                counter++
                totalDispatchedFrames++
                cleanEmptyGroupsLocked()
                updateStatsLocked()
            }
        }

        // Execute collected actions outside lock to prevent deadlocks
        for (action in actionsToRun) {
            action()
        }

        for (tick in firedTicks) {
            _ticksFlow.tryEmit(tick)
        }

        return firedTicks
    }

    override fun getStats(): FpsContentStats = synchronized(lock) {
        _stats.value
    }

    override fun getSubscriptions(): List<FrameCallbackSubscription> = synchronized(lock) {
        ArrayList(allSubscriptions.values)
    }

    override fun reset() = synchronized(lock) {
        counter = 0L
        lastVsyncNs = 0L
        accumulatedVsyncNs = 0L
        totalDispatchedFrames = 0L
        oneShotCallbacks.clear()
        groups.clear()
        viewsToInvalidate.clear()
        drawablesToInvalidate.clear()
        drawablesToInvalidate30fps.clear()
        allSubscriptions.clear()
        updateStatsLocked()
    }

    override fun observeStats(): StateFlow<FpsContentStats> = _stats.asStateFlow()

    override fun observeTicks(): Flow<FrameTick> = _ticksFlow.asSharedFlow()

    private fun getOrCreateGroup(fps: Int): CallbackGroupHolder {
        var group = groups[fps]
        if (group == null) {
            val config = FpsContentMapper.toFpsGroupConfig(fps)
            group = CallbackGroupHolder(config.fps, config.intervalNs, config.stride)
            groups[fps] = group
        }
        return group
    }

    private fun cleanEmptyGroupsLocked() {
        val iterator = groups.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isEmpty) {
                iterator.remove()
            }
        }
    }

    private fun updateStatsLocked() {
        _stats.value = FpsContentStats(
            isRunning = true,
            targetFps = FpsTimingUtils.TARGET_FPS,
            activeGroupsCount = groups.size,
            totalSubscriptionsCount = allSubscriptions.size,
            totalDispatchedFrames = totalDispatchedFrames,
            pendingOneShotCount = oneShotCallbacks.size,
            pendingDrawablesCount = drawablesToInvalidate.size,
            pendingDrawables30fpsCount = drawablesToInvalidate30fps.size,
            pendingViewsCount = viewsToInvalidate.size,
            currentVsyncAccumulatorNs = accumulatedVsyncNs
        )
    }
}
