package org.telegram.messenger.feature.botstars.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository

class ObserveBotTransactionsUseCase(
    private val repository: BotStarsRepository
) {
    operator fun invoke(dialogId: Long, type: BotStarsTransactionType = BotStarsTransactionType.ALL): Flow<List<BotStarsTransactionModel>> {
        return repository.observeTransactions(dialogId, type)
    }
}
