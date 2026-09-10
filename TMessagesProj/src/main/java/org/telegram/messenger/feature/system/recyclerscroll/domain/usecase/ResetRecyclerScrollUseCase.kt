package org.telegram.messenger.feature.system.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

class ResetRecyclerScrollUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
