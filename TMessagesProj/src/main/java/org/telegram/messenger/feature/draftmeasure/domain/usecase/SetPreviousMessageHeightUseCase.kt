package org.telegram.messenger.feature.draftmeasure.domain.usecase

import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class SetPreviousMessageHeightUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(height: Int) {
        repository.setPreviousMessageHeight(height)
    }
}
