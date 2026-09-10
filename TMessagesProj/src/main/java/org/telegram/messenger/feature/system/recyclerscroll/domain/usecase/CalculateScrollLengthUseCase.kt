package org.telegram.messenger.feature.system.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

class CalculateScrollLengthUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(
        scrollDown: Boolean,
        containerHeight: Int,
        oldViewsCount: Int,
        scrollDiff: Int,
        oldTop: Int,
        oldBottom: Int,
        incomingTop: Int,
        incomingBottom: Int
    ): Int = repository.calculateScrollLength(
        scrollDown = scrollDown,
        containerHeight = containerHeight,
        oldViewsCount = oldViewsCount,
        scrollDiff = scrollDiff,
        oldTop = oldTop,
        oldBottom = oldBottom,
        incomingTop = incomingTop,
        incomingBottom = incomingBottom
    )
}
