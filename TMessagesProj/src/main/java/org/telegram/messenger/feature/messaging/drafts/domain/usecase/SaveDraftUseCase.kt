package org.telegram.messenger.feature.messaging.drafts.domain.usecase

import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.messaging.drafts.domain.repository.DraftsRepository

class SaveDraftUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(draft: StoryDraftModel) {
        repository.saveDraft(draft)
    }
}
