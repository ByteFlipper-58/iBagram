package org.telegram.messenger.feature.business.billing.domain.usecase

import org.telegram.messenger.feature.business.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository

class GetPremiumProductUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): BillingProductModel? = repository.getPremiumProduct()
}
