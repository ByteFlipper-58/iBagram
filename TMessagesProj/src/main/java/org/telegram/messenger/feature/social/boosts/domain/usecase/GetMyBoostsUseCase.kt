package org.telegram.messenger.feature.social.boosts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository

class GetMyBoostsUseCase(
    private val repository: BoostsRepository
) {
    suspend operator fun invoke(): Result<MyBoostsModel> {
        return repository.getMyBoosts()
    }
}
