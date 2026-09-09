package org.telegram.messenger.feature.billing.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository

class StartBillingConnectionUseCase(
    private val repository: BillingRepository
) {
    suspend operator fun invoke(): Result<Boolean> = repository.startConnection()
}
