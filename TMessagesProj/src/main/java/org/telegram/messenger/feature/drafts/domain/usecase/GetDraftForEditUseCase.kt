package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class GetDraftForEditUseCase(
    private val repository: DraftsRepository
) {
    operator fun invoke(peerId: Long, storyId: Int): StoryDraftModel? {
        return repository.getDraftForEdit(peerId, storyId)
    }
}
