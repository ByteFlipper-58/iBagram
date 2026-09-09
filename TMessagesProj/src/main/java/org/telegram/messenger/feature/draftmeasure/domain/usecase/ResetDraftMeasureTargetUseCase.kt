package org.telegram.messenger.feature.draftmeasure.domain.usecase

import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class ResetDraftMeasureTargetUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(): Boolean {
        return repository.resetTarget()
    }
}
