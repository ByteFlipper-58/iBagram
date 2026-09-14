package org.telegram.messenger.feature.system.fpscontent.data.datasource

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.system.fpscontent.domain.model.FpsTimingUtils
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameCallbackType
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameTick
import java.util.concurrent.atomic.AtomicLong

/**
 * Local data source encapsulating frame rate arbitration, stride groups,
 * accumulator groups, and view/drawable invalidation scheduling.
 */
class FpsContentLocalDataSource {

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
    val stats: StateFlow<FpsContentStats> = _stats.asStateFlow()

    private val _ticksFlow = MutableSharedFlow<FrameTick>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val ticksFlow: Flow<FrameTick> = _ticksFlow.asSharedFlow()

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

    fun addFrameCallback(fps: Int = 60, isOneShot: Boolean = false, onFrame: (Long) -> Unit): String = synchronized(lock) {
        val safeFps = FpsTimingUtils.clampFps(fps)
        val id = "frame_sub_${idGenerator.incrementAndGet()}"
        val sub = FrameCallbackSubscription(
            id = id,
            fps = safeFps,
            isOneShot = isOneShot,
            type = FrameCallbackType.FRAME_TICK,
            registeredAtNanos = System.nanoTime()
        )

        if (isOneShot && safeFps == FpsTimingUtils.TARGET_FPS) {
            oneShotCallbacks[id] = onFrame
        } else {
            val group = getOrCreateGroup(safeFps)
            group.callbacks[id] = onFrame
        }

        allSubscriptions[id] = sub
        updateStatsLocked()
        id
    }

    fun addRunnableCallback(fps: Int = 60, isOneShot: Boolean = false, action: () -> Unit): String = synchronized(lock) {
        val safeFps = FpsTimingUtils.clampFps(fps)
        val id = "runnable_sub_${idGenerator.incrementAndGet()}"
        val sub = FrameCallbackSubscription(
            id = id,
            fps = safeFps,
            isOneShot = isOneShot,
            type = FrameCallbackType.RUNNABLE,
            registeredAtNanos = System.nanoTime()
        )

        val group = getOrCreateGroup(safeFps)
        if (isOneShot) {
            group.runnablesOnce[id] = action
        } else {
            group.runnables[id] = action
        }

        allSubscriptions[id] = sub
        updateStatsLocked()
        id
    }

    fun removeCallback(subscriptionId: String): Boolean = synchronized(lock) {
        var removed = false
        if (oneShotCallbacks.remove(subscriptionId) != null) {
            removed = true
        }

        val it = groups.values.iterator()
        while (it.hasNext()) {
            val group = it.next()
            val r1 = group.callbacks.remove(subscriptionId) != null
            val r2 = group.runnables.remove(subscriptionId) != null
            val r3 = group.runnablesOnce.remove(subscriptionId) != null
            if (r1 || r2 || r3) {
                removed = true
            }
            if (group.isEmpty) {
                it.remove()
            }
        }

        allSubscriptions.remove(subscriptionId)
        if (removed) {
            updateStatsLocked()
        }
        removed
    }

    fun postInvalidateView(viewId: String): Boolean = synchronized(lock) {
        val added = viewsToInvalidate.add(viewId)
        if (added) updateStatsLocked()
        added
    }

    fun postInvalidateDrawable(drawableId: String, fps: Int = 60): Boolean = synchronized(lock) {
        val added = if (fps <= 30) {
            drawablesToInvalidate30fps.add(drawableId)
        } else {
            drawablesToInvalidate.add(drawableId)
        }
        if (added) updateStatsLocked()
        added
    }

    fun dispatchVsync(frameTimeNanos: Long): List<FrameTick> {
        val ticksToEmit = mutableListOf<FrameTick>()
        val actionsToRun = mutableListOf<() -> Unit>()

        synchronized(lock) {
            val deltaNs = if (lastVsyncNs == 0L) {
                FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
            } else {
                (frameTimeNanos - lastVsyncNs).coerceAtLeast(0L)
            }
            lastVsyncNs = frameTimeNanos

            accumulatedVsyncNs += deltaNs
            var targetFramesToDispatch = (accumulatedVsyncNs / FpsTimingUtils.TARGET_FRAME_INTERVAL_NS).toInt()
            if (targetFramesToDispatch > 0) {
                accumulatedVsyncNs %= FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
            } else {
                targetFramesToDispatch = 1
            }

            for (f in 0 until targetFramesToDispatch) {
                counter++
                totalDispatchedFrames++

                if (oneShotCallbacks.isNotEmpty()) {
                    val oneShots = ArrayList(oneShotCallbacks.values)
                    oneShotCallbacks.clear()
                    allSubscriptions.entries.removeIf { it.value.isOneShot && it.value.fps == FpsTimingUtils.TARGET_FPS }
                    actionsToRun.add {
                        for (cb in oneShots) cb.invoke(frameTimeNanos)
                    }
                }

                if (viewsToInvalidate.isNotEmpty()) {
                    viewsToInvalidate.clear()
                }
                if (drawablesToInvalidate.isNotEmpty()) {
                    drawablesToInvalidate.clear()
                }
                if ((counter % 2) == 0L && drawablesToInvalidate30fps.isNotEmpty()) {
                    drawablesToInvalidate30fps.clear()
                }

                for (group in groups.values) {
                    val (fire, updatedAcc) = FpsTimingUtils.shouldFire(
                        counter = counter,
                        stride = group.stride,
                        currentAccumulatedNs = group.accumulatedNs,
                        intervalNs = group.intervalNs,
                        tickIntervalNs = FpsTimingUtils.TARGET_FRAME_INTERVAL_NS
                    )
                    group.accumulatedNs = updatedAcc

                    if (fire) {
                        val tick = FrameTick(
                            frameTimeNanos = frameTimeNanos,
                            counter = counter,
                            fps = group.fps,
                            isStrideMatch = group.stride > 0
                        )
                        ticksToEmit.add(tick)

                        if (group.callbacks.isNotEmpty()) {
                            val list = ArrayList(group.callbacks.values)
                            actionsToRun.add {
                                for (cb in list) cb.invoke(frameTimeNanos)
                            }
                        }

                        if (group.runnables.isNotEmpty()) {
                            val rList = ArrayList(group.runnables.values)
                            actionsToRun.add {
                                for (r in rList) r.invoke()
                            }
                        }

                        if (group.runnablesOnce.isNotEmpty()) {
                            val rOnce = ArrayList(group.runnablesOnce.values)
                            val ids = ArrayList(group.runnablesOnce.keys)
                            group.runnablesOnce.clear()
                            for (id in ids) allSubscriptions.remove(id)
                            actionsToRun.add {
                                for (r in rOnce) r.invoke()
                            }
                        }
                    }
                }
            }

            updateStatsLocked()
        }

        for (tick in ticksToEmit) {
            _ticksFlow.tryEmit(tick)
        }

        for (action in actionsToRun) {
            action.invoke()
        }

        return ticksToEmit
    }

    fun getStats(): FpsContentStats = synchronized(lock) {
        _stats.value
    }

    fun getSubscriptions(): List<FrameCallbackSubscription> = synchronized(lock) {
        allSubscriptions.values.toList()
    }

    fun reset() = synchronized(lock) {
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

    fun observeStats(): StateFlow<FpsContentStats> = stats

    fun observeTicks(): Flow<FrameTick> = ticksFlow

    private fun getOrCreateGroup(fps: Int): CallbackGroupHolder {
        return groups.computeIfAbsent(fps) {
            val interval = FpsTimingUtils.calculateIntervalNs(fps)
            val stride = FpsTimingUtils.calculateStride(FpsTimingUtils.TARGET_FPS, fps)
            CallbackGroupHolder(fps, interval, stride)
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
