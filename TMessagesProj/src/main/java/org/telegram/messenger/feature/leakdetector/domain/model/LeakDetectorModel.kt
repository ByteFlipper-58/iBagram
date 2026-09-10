package org.telegram.messenger.feature.leakdetector.domain.model

/**
 * Statistics for a single tracked class or component tag.
 */
data class TrackedClassStats(
    val className: String,
    val liveCount: Int,
    val isSuspicious: Boolean = false,
    val isConfirmedLeak: Boolean = false,
    val isPendingRecheck: Boolean = false
)

/**
 * Snapshot report of a confirmed memory leak.
 */
data class LeakReport(
    val className: String,
    val instanceCount: Int,
    val timestampMs: Long = System.currentTimeMillis(),
    val threshold: Int = 5
)

/**
 * Configuration parameters for the Leak Detector engine.
 */
data class LeakDetectorConfig(
    val leakThreshold: Int = 5,
    val checkIntervalMs: Long = 1000L,
    val gcRecheckDelayMs: Long = 2000L
)

/**
 * Aggregated reactive state of memory leak monitoring.
 */
data class LeakDetectorState(
    val isRunning: Boolean = false,
    val trackedClassesCount: Int = 0,
    val totalLiveInstances: Int = 0,
    val suspiciousClassesCount: Int = 0,
    val confirmedLeaks: List<LeakReport> = emptyList(),
    val pendingRecheckClasses: List<String> = emptyList(),
    val lastCheckTimestampMs: Long = 0L
)
