package org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase

import org.telegram.messenger.feature.messaging.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository

class GetDraftMeasureConfigUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(): DraftMeasureConfig {
        return repository.getConfig()
    }
}
