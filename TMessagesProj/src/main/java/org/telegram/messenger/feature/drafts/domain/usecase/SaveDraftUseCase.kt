package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class SaveDraftUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(draft: StoryDraftModel) {
        repository.saveDraft(draft)
    }
}
