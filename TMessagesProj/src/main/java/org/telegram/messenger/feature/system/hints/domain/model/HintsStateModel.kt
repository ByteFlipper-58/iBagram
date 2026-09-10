package org.telegram.messenger.feature.system.hints.domain.model

/**
 * Pure domain model encapsulating the complete state of all user hints.
 */
data class HintsStateModel(
    val hints: Map<HintType, HintModel> = emptyMap()
) {
    fun getHint(type: HintType): HintModel {
        return hints[type] ?: HintModel(type = type)
    }

    val activeHintsCount: Int
        get() = hints.values.count { !it.isLimitReached }
}
