package org.telegram.messenger.feature.billing.domain.usecase

import org.telegram.messenger.feature.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class GetBillingStateUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): BillingStateModel = repository.getBillingState()
}
