package org.telegram.messenger.feature.messaging.drafts.domain.usecase

import org.telegram.messenger.feature.messaging.drafts.domain.repository.DraftsRepository

class DeleteDraftUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(draftId: Long) {
        repository.deleteDraft(draftId)
    }

    suspend operator fun invoke(draftIds: List<Long>) {
        repository.deleteDrafts(draftIds)
    }
}
