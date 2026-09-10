package org.telegram.messenger.feature.system.hints.domain.model

/**
 * Pure domain model representing the current state and policy of an individual hint.
 */
data class HintModel(
    val type: HintType,
    val showsCount: Int = 0,
    val showsLimit: Int = type.showsLimit,
    val probability: Float = type.probability
) {
    val isLimitReached: Boolean
        get() = showsCount >= showsLimit

    fun canShow(randomValue: Float): Boolean {
        if (showsCount >= showsLimit) {
            return false
        }
        if (probability >= 1.0f) {
            return true
        }
        if (probability <= 0.0f) {
            return false
        }
        return randomValue < probability
    }
}
