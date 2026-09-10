package org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase

import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository

class SetDraftMeasureTargetUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(messageId: Int, groupId: Long = 0L): Boolean {
        return repository.setTarget(messageId, groupId)
    }
}
