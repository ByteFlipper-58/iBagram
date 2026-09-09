package org.telegram.messenger.feature.autodelete.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.autodelete.domain.repository.AutoDeleteRepository

class SetGlobalAutoDeleteUseCase(
    private val repository: AutoDeleteRepository
) {
    suspend operator fun invoke(ttl: AutoDeleteTtlModel): Result<Unit> {
        return repository.setGlobalAutoDelete(ttl)
    }
}
