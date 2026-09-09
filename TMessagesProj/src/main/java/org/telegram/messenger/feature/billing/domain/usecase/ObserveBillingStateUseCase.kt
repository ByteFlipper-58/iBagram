package org.telegram.messenger.feature.billing.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class ObserveBillingStateUseCase(
    private val repository: BillingRepository
) {
    operator fun invoke(): Flow<BillingStateModel> = repository.observeBillingState()
}
