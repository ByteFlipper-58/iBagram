package org.telegram.messenger.feature.messaging.factcheck.domain.usecase

import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckModel
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository

class GetFactCheckUseCase(
    private val repository: FactCheckRepository
) {
    suspend operator fun invoke(dialogId: Long, messageId: Int, hash: Long): FactCheckModel? {
        return repository.getFactCheck(dialogId, messageId, hash)
    }
}
