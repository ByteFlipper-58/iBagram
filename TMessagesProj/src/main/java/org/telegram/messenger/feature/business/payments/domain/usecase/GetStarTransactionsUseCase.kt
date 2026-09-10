package org.telegram.messenger.feature.business.payments.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository

class GetStarTransactionsUseCase(
    private val repository: PaymentsRepository
) {
    suspend operator fun invoke(type: Int = 0): Result<List<StarTransactionModel>> =
        repository.getTransactions(type)
}
