package org.telegram.messenger.feature.autodelete.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.autodelete.domain.repository.AutoDeleteRepository

class SetChatsAutoDeleteBatchUseCase(
    private val repository: AutoDeleteRepository
) {
    suspend operator fun invoke(chatIds: List<Long>, ttl: AutoDeleteTtlModel): Result<Unit> {
        return repository.setChatsAutoDeleteBatch(chatIds, ttl)
    }
}
