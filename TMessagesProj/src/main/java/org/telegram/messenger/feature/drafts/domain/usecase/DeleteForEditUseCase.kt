package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class DeleteForEditUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(peerId: Long, storyId: Int) {
        repository.deleteForEdit(peerId, storyId)
    }
}
