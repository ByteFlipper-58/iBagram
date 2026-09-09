package org.telegram.messenger.feature.draftmeasure.domain.usecase

import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class OnDraftMessageIdChangedUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(oldMessageId: Int, newMessageId: Int, groupId: Long = 0L): Boolean {
        return repository.onMessageIdChanged(oldMessageId, newMessageId, groupId)
    }
}
