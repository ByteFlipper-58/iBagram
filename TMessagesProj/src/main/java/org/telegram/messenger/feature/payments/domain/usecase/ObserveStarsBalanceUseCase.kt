package org.telegram.messenger.feature.payments.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.payments.domain.model.StarsBalanceModel
import org.telegram.messenger.feature.payments.domain.repository.PaymentsRepository

class ObserveStarsBalanceUseCase(
    private val repository: PaymentsRepository
) {
    operator fun invoke(): Flow<StarsBalanceModel> = repository.observeBalance()
}
