package org.telegram.messenger.feature.windowvisibility.data.mapper

import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityChangeResult
import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityState

/**
 * Pure mapper for window visibility states and transitions.
 */
object WindowVisibilityMapper {

    fun toState(
        reasonsCount: Int,
        activeReasons: Set<String>,
        lastChangedReason: String? = null
    ): WindowVisibilityState {
        val count = reasonsCount.coerceAtLeast(0)
        return WindowVisibilityState(
            isVisible = count == 0,
            reasonsCount = count,
            activeReasons = activeReasons.toSet(),
            lastChangedReason = lastChangedReason
        )
    }

    fun toLegacyIsHidden(state: WindowVisibilityState): Boolean {
        return state.isHidden
    }

    fun calculateChangeResult(
        previousState: WindowVisibilityState,
        newState: WindowVisibilityState
    ): WindowVisibilityChangeResult {
        return WindowVisibilityChangeResult(
            previousState = previousState,
            newState = newState,
            visibilityToggled = previousState.isVisible != newState.isVisible
        )
    }
}
