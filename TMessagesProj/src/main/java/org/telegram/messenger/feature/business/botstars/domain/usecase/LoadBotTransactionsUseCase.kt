package org.telegram.messenger.feature.business.botstars.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository

class LoadBotTransactionsUseCase(
    private val repository: BotStarsRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        type: BotStarsTransactionType = BotStarsTransactionType.ALL,
        reload: Boolean = false
    ): Result<List<BotStarsTransactionModel>> {
        return repository.loadTransactions(dialogId, type, reload)
    }
}
