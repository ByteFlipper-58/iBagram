package org.telegram.messenger.feature.business.billing.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository

class ManageSubscriptionUseCase(
    private val repository: BillingRepository
) {
    suspend operator fun invoke(productId: String = "telegram_premium"): Result<Boolean> {
        return repository.manageSubscription(productId)
    }
}
