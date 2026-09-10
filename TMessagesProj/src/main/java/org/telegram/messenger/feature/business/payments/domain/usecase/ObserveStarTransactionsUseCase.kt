package org.telegram.messenger.feature.business.payments.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository

class ObserveStarTransactionsUseCase(
    private val repository: PaymentsRepository
) {
    operator fun invoke(): Flow<List<StarTransactionModel>> = repository.observeTransactions()
}
