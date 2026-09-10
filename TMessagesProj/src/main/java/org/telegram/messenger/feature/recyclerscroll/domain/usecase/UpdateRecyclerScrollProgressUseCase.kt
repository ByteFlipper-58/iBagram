package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class UpdateRecyclerScrollProgressUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(progress: Float) {
        repository.updateProgress(progress)
    }
}
