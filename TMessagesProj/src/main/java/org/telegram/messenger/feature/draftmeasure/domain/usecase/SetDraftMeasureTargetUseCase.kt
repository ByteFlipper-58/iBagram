package org.telegram.messenger.feature.draftmeasure.domain.usecase

import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class SetDraftMeasureTargetUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(messageId: Int, groupId: Long = 0L): Boolean {
        return repository.setTarget(messageId, groupId)
    }
}
