package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class GetDraftsStateUseCase(
    private val repository: DraftsRepository
) {
    operator fun invoke(): DraftsStateModel = repository.getDraftsState()
}
