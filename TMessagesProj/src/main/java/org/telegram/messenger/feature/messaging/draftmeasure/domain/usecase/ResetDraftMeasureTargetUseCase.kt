package org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase

import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository

class ResetDraftMeasureTargetUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(): Boolean {
        return repository.resetTarget()
    }
}
