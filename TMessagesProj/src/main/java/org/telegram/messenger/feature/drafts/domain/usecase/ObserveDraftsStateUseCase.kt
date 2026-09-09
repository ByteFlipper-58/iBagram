package org.telegram.messenger.feature.drafts.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class ObserveDraftsStateUseCase(
    private val repository: DraftsRepository
) {
    operator fun invoke(): Flow<DraftsStateModel> = repository.observeDraftsState()
}
