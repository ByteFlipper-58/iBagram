package org.telegram.messenger.feature.pip.domain.model

/**
 * Domain model representing a candidate video or stream source for Picture-in-Picture mode.
 */
data class PipSourceModel(
    val tag: String,
    val priority: Int = 0,
    val isAvailable: Boolean = true,
    val isAttachedToPip: Boolean = false,
    val needsMediaSession: Boolean = false,
    val aspectRatioWidth: Int = 16,
    val aspectRatioHeight: Int = 9,
    val title: String? = null
) {
    val aspectRatio: Float
        get() = if (aspectRatioHeight > 0) aspectRatioWidth.toFloat() / aspectRatioHeight.toFloat() else 1.777f

    val isEligible: Boolean
        get() = isAvailable || isAttachedToPip
}
