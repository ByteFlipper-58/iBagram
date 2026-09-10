package org.telegram.messenger.feature.system.fpscontent.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameTick

/**
 * Domain repository contract for 60fps animation callbacks, v-sync timing,
 * and view/drawable invalidation scheduling.
 */
interface FpsContentRepository {

    /**
     * Registers a persistent or one-shot callback that receives the frame timestamp in nanoseconds.
     */
    fun addFrameCallback(
        fps: Int = 60,
        isOneShot: Boolean = false,
        onFrame: (Long) -> Unit
    ): String

    /**
     * Registers a persistent or one-shot runnable action.
     */
    fun addRunnableCallback(
        fps: Int = 60,
        isOneShot: Boolean = false,
        action: () -> Unit
    ): String

    /**
     * Unregisters a previously registered callback by its ID.
     */
    fun removeCallback(subscriptionId: String): Boolean

    /**
     * Enqueues a view invalidation for the next ~60 fps tick.
     */
    fun postInvalidateView(viewId: String): Boolean

    /**
     * Enqueues a drawable invalidation for the next ~60 fps or legacy 30 fps tick.
     */
    fun postInvalidateDrawable(drawableId: String, fps: Int = 60): Boolean

    /**
     * Dispatches an incoming display VSYNC tick (in nanoseconds).
     * Dispatches target frames if accumulated time >= FRAME_INTERVAL_NS.
     * Returns list of fired frame ticks.
     */
    fun dispatchVsync(frameTimeNanos: Long): List<FrameTick>

    /**
     * Returns a snapshot of current arbitration statistics.
     */
    fun getStats(): FpsContentStats

    /**
     * Returns all active callback subscriptions.
     */
    fun getSubscriptions(): List<FrameCallbackSubscription>

    /**
     * Clears all callbacks, pending invalidations, and resets frame counters.
     */
    fun reset()

    /**
     * Observes real-time state flow of statistics.
     */
    fun observeStats(): StateFlow<FpsContentStats>

    /**
     * Hot stream of dispatched frame ticks.
     */
    fun observeTicks(): Flow<FrameTick>
}
