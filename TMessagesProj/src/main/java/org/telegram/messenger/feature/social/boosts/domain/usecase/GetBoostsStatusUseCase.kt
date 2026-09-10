package org.telegram.messenger.feature.social.boosts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository

class GetBoostsStatusUseCase(
    private val repository: BoostsRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<BoostStatusModel> {
        return repository.getBoostsStatus(dialogId)
    }
}
