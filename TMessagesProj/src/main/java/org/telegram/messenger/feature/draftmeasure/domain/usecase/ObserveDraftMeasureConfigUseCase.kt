package org.telegram.messenger.feature.draftmeasure.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureConfig
import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class ObserveDraftMeasureConfigUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(): Flow<DraftMeasureConfig> {
        return repository.observeConfig()
    }
}
