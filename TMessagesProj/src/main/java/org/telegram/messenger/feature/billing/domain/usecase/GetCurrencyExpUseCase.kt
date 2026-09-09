package org.telegram.messenger.feature.billing.domain.usecase

import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class GetCurrencyExpUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(currency: String): Int = repository.getCurrencyExp(currency)
}
