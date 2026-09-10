package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class CancelRecyclerScrollUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke() {
        repository.cancelScroll()
    }
}
