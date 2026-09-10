package org.telegram.messenger.feature.messaging.factcheck.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckModel
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository

class LoadFactCheckUseCase(
    private val repository: FactCheckRepository
) {
    suspend operator fun invoke(dialogId: Long, messageId: Int): Result<FactCheckModel?> {
        return repository.loadFactCheck(dialogId, messageId)
    }
}
