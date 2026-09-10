package org.telegram.messenger.feature.system.countdowntimer.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerState
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerTick

/**
 * Repository contract for starting, stopping, and observing countdown timers.
 */
interface CountdownTimerRepository {
    /**
     * Starts or restarts a countdown timer with the specified duration in seconds.
     */
    fun start(timerId: String, seconds: Long): CountdownTimerTick

    /**
     * Stops and cancels a running timer.
     */
    fun stop(timerId: String): CountdownTimerTick?

    /**
     * Pauses a running timer without resetting its remaining seconds.
     */
    fun pause(timerId: String): CountdownTimerTick?

    /**
     * Resumes a paused timer.
     */
    fun resume(timerId: String): CountdownTimerTick?

    /**
     * Gets the snapshot of a specific timer.
     */
    fun getTimer(timerId: String): CountdownTimerTick?

    /**
     * Checks if a timer is currently active and running.
     */
    fun isRunning(timerId: String): Boolean

    /**
     * Manually decrements a timer by the specified step (default 1 second).
     * Useful for deterministic testing or custom tick drivers.
     */
    fun tick(timerId: String, stepSeconds: Long = 1): CountdownTimerTick?

    /**
     * Clears all registered timers.
     */
    fun clearAll(): CountdownTimerState

    /**
     * Observes ticks for a specific timer.
     */
    fun observeTimer(timerId: String): Flow<CountdownTimerTick>

    /**
     * Observes the aggregated timer state across all active timers.
     */
    fun observeState(): StateFlow<CountdownTimerState>
}
