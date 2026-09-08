package org.telegram.messenger.feature.boosts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.boosts.domain.repository.BoostsRepository

class CheckCanApplyBoostUseCase(
    private val repository: BoostsRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<CanApplyBoostModel> {
        return repository.checkCanApplyBoost(dialogId)
    }
}
