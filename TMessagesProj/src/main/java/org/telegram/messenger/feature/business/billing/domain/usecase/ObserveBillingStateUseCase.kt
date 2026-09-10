package org.telegram.messenger.feature.business.billing.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository

class ObserveBillingStateUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): Flow<BillingStateModel> = repository.observeBillingState()
}
