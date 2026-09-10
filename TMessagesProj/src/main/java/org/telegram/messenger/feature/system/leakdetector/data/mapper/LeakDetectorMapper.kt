package org.telegram.messenger.feature.system.leakdetector.data.mapper

import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.system.leakdetector.domain.model.TrackedClassStats

/**
 * Pure Kotlin mapper for leak detector transformations and state aggregations.
 */
object LeakDetectorMapper {

    fun buildClassStats(
        className: String,
        liveCount: Int,
        threshold: Int,
        isConfirmedLeak: Boolean,
        isPendingRecheck: Boolean
    ): TrackedClassStats {
        return TrackedClassStats(
            className = className,
            liveCount = liveCount,
            isSuspicious = liveCount >= threshold,
            isConfirmedLeak = isConfirmedLeak,
            isPendingRecheck = isPendingRecheck
        )
    }

    fun buildLeakReport(
        className: String,
        instanceCount: Int,
        threshold: Int
    ): LeakReport {
        return LeakReport(
            className = className,
            instanceCount = instanceCount,
            timestampMs = System.currentTimeMillis(),
            threshold = threshold
        )
    }

    fun buildState(
        isRunning: Boolean,
        stats: List<TrackedClassStats>,
        confirmedLeaks: List<LeakReport>,
        pendingClasses: Set<String>
    ): LeakDetectorState {
        val totalLive = stats.sumOf { it.liveCount }
        val suspicious = stats.count { it.isSuspicious && !it.isConfirmedLeak }
        return LeakDetectorState(
            isRunning = isRunning,
            trackedClassesCount = stats.size,
            totalLiveInstances = totalLive,
            suspiciousClassesCount = suspicious,
            confirmedLeaks = confirmedLeaks,
            pendingRecheckClasses = pendingClasses.toList(),
            lastCheckTimestampMs = System.currentTimeMillis()
        )
    }
}
