package org.telegram.messenger.feature.boosts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.boosts.domain.model.MyBoostsModel
import org.telegram.messenger.feature.boosts.domain.repository.BoostsRepository

class ApplyBoostUseCase(
    private val repository: BoostsRepository
) {
    suspend operator fun invoke(dialogId: Long, slots: List<Int>): Result<MyBoostsModel> {
        return repository.applyBoost(dialogId, slots)
    }
}
