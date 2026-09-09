package org.telegram.messenger.feature.refreshrate.domain.model

/**
 * Pure domain model representing a display refresh mode (resolution and Hz).
 */
data class DisplayRefreshModeModel(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float
) {
    val isApproximately60Hz: Boolean
        get() = refreshRate in 58.0f..62.5f

    val isHighRefreshRate: Boolean
        get() = refreshRate > 62.5f
}
