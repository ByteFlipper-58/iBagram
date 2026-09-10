package org.telegram.messenger.feature.business.billing.domain.usecase

import org.telegram.messenger.feature.business.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository

class GetBillingStateUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): BillingStateModel = repository.getBillingState()
}
