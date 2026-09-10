package org.telegram.messenger.feature.messaging.draftmeasure.domain.model

/**
 * Target message or grouped media under draft height override measurement.
 */
data class DraftMeasureTarget(
    val messageId: Int = 0,
    val groupId: Long = 0L
) {
    val isActive: Boolean
        get() = messageId > 0 || groupId != 0L

    fun matches(targetMessageId: Int, targetGroupId: Long): Boolean {
        if (messageId > 0 && messageId == targetMessageId) {
            return true
        }
        if (groupId != 0L && groupId == targetGroupId) {
            return true
        }
        return false
    }

    companion object {
        val EMPTY = DraftMeasureTarget(0, 0L)
    }
}

/**
 * Viewport constraints for draft message height measurement.
 */
data class DraftMeasureViewport(
    val totalHeight: Int,
    val paddingTop: Int = 0,
    val paddingBottom: Int = 0,
    val previousMessageHeight: Int = 0
) {
    val availableHeight: Int
        get() = (totalHeight - paddingTop - paddingBottom - previousMessageHeight).coerceAtLeast(0)
}

/**
 * Result of draft message height override calculation.
 */
data class DraftMeasureResult(
    val measuredHeight: Int,
    val additionalHeight: Int,
    val finalHeight: Int,
    val hasAdditionalHeight: Boolean
)

/**
 * Complete snapshot of draft measurement configuration.
 */
data class DraftMeasureConfig(
    val target: DraftMeasureTarget = DraftMeasureTarget.EMPTY,
    val previousMessageHeight: Int = 0,
    val hasAdditionalHeight: Boolean = false
)
