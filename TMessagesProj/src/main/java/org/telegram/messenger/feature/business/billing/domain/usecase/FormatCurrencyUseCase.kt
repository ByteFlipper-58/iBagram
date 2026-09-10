package org.telegram.messenger.feature.business.billing.domain.usecase

import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository

class FormatCurrencyUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(amount: Long, currency: String, exp: Int = 0, rounded: Boolean = false): String {
        return repository.formatCurrency(amount, currency, exp, rounded)
    }
}
