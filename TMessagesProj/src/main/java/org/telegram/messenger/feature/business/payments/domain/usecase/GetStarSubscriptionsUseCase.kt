package org.telegram.messenger.feature.business.payments.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository

class GetStarSubscriptionsUseCase(
    private val repository: PaymentsRepository
) {
    suspend operator fun invoke(): Result<List<StarSubscriptionModel>> =
        repository.getSubscriptions()
}
