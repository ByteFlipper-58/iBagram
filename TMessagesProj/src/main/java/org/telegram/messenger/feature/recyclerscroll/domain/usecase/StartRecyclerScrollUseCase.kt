package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class StartRecyclerScrollUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(
        position: Int,
        offset: Int,
        direction: ScrollDirection,
        durationMs: Long
    ) {
        repository.startScroll(position, offset, direction, durationMs)
    }
}
