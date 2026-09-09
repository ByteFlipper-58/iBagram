package org.telegram.messenger.feature.draftmeasure.domain.usecase

import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureResult
import org.telegram.messenger.feature.draftmeasure.domain.model.DraftMeasureViewport
import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository

class CalculateDraftMeasureOverrideUseCase(
    private val repository: DraftMeasureRepository
) {
    operator fun invoke(
        messageId: Int,
        groupId: Long,
        measuredHeight: Int,
        viewport: DraftMeasureViewport
    ): DraftMeasureResult {
        return repository.calculateOverrideHeight(messageId, groupId, measuredHeight, viewport)
    }
}
