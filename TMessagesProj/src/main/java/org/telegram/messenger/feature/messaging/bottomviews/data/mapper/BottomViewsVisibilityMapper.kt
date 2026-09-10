package org.telegram.messenger.feature.messaging.bottomviews.data.mapper

import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomViewsVisibilityState

object BottomViewsVisibilityMapper {

    fun calculatePriorityContainerId(flags: Int): Int {
        if (flags == 0) return 0
        return 31 - java.lang.Integer.numberOfLeadingZeros(flags)
    }

    fun toState(flags: Int, priorityId: Int, visibilities: Map<Int, Float>): BottomViewsVisibilityState {
        return BottomViewsVisibilityState(
            visibilityFlags = flags,
            priorityContainerId = priorityId,
            visibilities = visibilities
        )
    }
}
