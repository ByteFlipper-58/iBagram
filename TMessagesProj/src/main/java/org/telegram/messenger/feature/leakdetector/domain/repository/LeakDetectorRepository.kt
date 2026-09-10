package org.telegram.messenger.feature.leakdetector.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.leakdetector.domain.model.TrackedClassStats

/**
 * Domain contract for memory leak detection, instance tracking, and leak reporting.
 */
interface LeakDetectorRepository {

    /**
     * Starts periodic leak scanning.
     */
    fun start(config: LeakDetectorConfig = LeakDetectorConfig())

    /**
     * Stops periodic leak scanning.
     */
    fun stop()

    /**
     * Registers an object instance for memory leak tracking using a weak reference.
     */
    fun track(tagOrClassName: String, instance: Any)

    /**
     * Manually triggers a scan over tracked classes and initiates two-phase recheck if threshold exceeded.
     */
    fun triggerCheck(): List<LeakReport>

    /**
     * Confirms a leak for a given class after GC recheck window.
     */
    fun confirmLeak(className: String): LeakReport?

    /**
     * Returns the live instance count for a tracked class.
     */
    fun getLiveCount(className: String): Int

    /**
     * Returns the list of all confirmed leaks.
     */
    fun getReportedLeaks(): List<LeakReport>

    /**
     * Returns statistics for all currently tracked classes.
     */
    fun getTrackedStats(): List<TrackedClassStats>

    /**
     * Resets the detector state, clears all tracked instances and reported leaks.
     */
    fun reset()

    /**
     * Reactive StateFlow stream of the leak detector state.
     */
    fun observeState(): StateFlow<LeakDetectorState>

    /**
     * Shared stream of newly confirmed memory leak reports.
     */
    fun observeLeaks(): Flow<LeakReport>
}
