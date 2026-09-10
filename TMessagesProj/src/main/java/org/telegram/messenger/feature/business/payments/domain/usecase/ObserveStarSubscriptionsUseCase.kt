package org.telegram.messenger.feature.business.payments.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository

class ObserveStarSubscriptionsUseCase(
    private val repository: PaymentsRepository
) {
    operator fun invoke(): Flow<List<StarSubscriptionModel>> = repository.observeSubscriptions()
}
