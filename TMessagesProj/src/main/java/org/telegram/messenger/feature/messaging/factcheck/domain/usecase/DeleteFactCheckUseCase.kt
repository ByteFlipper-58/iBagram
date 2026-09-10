package org.telegram.messenger.feature.messaging.factcheck.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository

class DeleteFactCheckUseCase(
    private val repository: FactCheckRepository
) {
    suspend operator fun invoke(dialogId: Long, messageId: Int): Result<Unit> {
        return repository.deleteFactCheck(dialogId, messageId)
    }
}
