package org.telegram.messenger.feature.billing.domain.usecase

import org.telegram.messenger.feature.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class GetPremiumProductUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): BillingProductModel? = repository.getPremiumProduct()
}
