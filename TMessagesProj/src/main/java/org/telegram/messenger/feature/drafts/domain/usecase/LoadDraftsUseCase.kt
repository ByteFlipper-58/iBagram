package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class LoadDraftsUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(force: Boolean = false) {
        repository.loadDrafts(force)
    }
}
