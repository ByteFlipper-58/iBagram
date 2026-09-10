package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class FinishRecyclerScrollUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke() {
        repository.finishScroll()
    }
}
