package org.telegram.messenger.feature.refreshrate.domain.model

/**
 * Hysteresis and stability tuning parameters for adaptive refresh rate switching.
 */
data class RefreshRateHysteresisConfig(
    val stableWindowMs: Long = 1800L,
    val minSwitchIntervalMs: Long = 3000L,
    val downFpsThreshold: Float = 55.0f,
    val upFpsThreshold: Float = 58.5f,
    val ringSize: Int = 240
)
