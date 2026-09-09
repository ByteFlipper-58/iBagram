package org.telegram.messenger.feature.bottomviews.domain.model

/**
 * Snapshot of bottom container visibility arbitration flags and computed alpha factors.
 */
data class BottomViewsVisibilityState(
    val visibilityFlags: Int = 1,
    val priorityContainerId: Int = 0,
    val visibilities: Map<Int, Float> = emptyMap()
) {
    fun getVisibility(containerId: Int): Float {
        return visibilities[containerId] ?: if (containerId == priorityContainerId && isContainerVisible(containerId)) 1.0f else 0.0f
    }

    fun isContainerVisible(containerId: Int): Boolean {
        if (containerId !in 0..31) return false
        return (visibilityFlags and (1 shl containerId)) != 0
    }

    val isInputVisible: Boolean
        get() = isContainerVisible(BottomContainerType.MESSAGE_INPUT.id)

    val isSearchVisible: Boolean
        get() = isContainerVisible(BottomContainerType.MESSAGE_SEARCH.id)

    val isActionVisible: Boolean
        get() = isContainerVisible(BottomContainerType.MESSAGE_ACTION.id)

    companion object {
        val DEFAULT = BottomViewsVisibilityState(
            visibilityFlags = 1,
            priorityContainerId = 0,
            visibilities = mapOf(0 to 1.0f)
        )
    }
}
