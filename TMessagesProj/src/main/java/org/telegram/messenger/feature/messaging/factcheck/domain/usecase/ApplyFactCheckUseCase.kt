package org.telegram.messenger.feature.messaging.factcheck.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository

class ApplyFactCheckUseCase(
    private val repository: FactCheckRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        messageId: Int,
        text: String,
        entities: List<FactCheckEntityModel>? = null
    ): Result<Unit> {
        return repository.applyFactCheck(dialogId, messageId, text, entities)
    }
}
