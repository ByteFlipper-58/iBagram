package org.telegram.messenger.feature.payments.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.payments.domain.repository.PaymentsRepository

class RefreshStarsBalanceUseCase(
    private val repository: PaymentsRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshBalance()
}
