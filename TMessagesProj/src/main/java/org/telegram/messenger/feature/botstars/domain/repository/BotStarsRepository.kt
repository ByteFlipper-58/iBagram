package org.telegram.messenger.feature.botstars.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.botstars.domain.model.StarRefProgramModel

interface BotStarsRepository {
    fun observeBotStarsStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?>
    suspend fun getBotStarsStats(dialogId: Long, force: Boolean = false): Result<BotStarsRevenueStatsModel?>

    fun observeTonStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?>
    suspend fun getTonStats(dialogId: Long, force: Boolean = false): Result<BotStarsRevenueStatsModel?>

    fun observeTransactions(dialogId: Long, type: BotStarsTransactionType): Flow<List<BotStarsTransactionModel>>
    suspend fun loadTransactions(dialogId: Long, type: BotStarsTransactionType, reload: Boolean = false): Result<List<BotStarsTransactionModel>>

    fun observeConnectedBots(dialogId: Long): Flow<List<ConnectedBotStarRefModel>>
    suspend fun loadConnectedBots(dialogId: Long, reload: Boolean = false): Result<List<ConnectedBotStarRefModel>>

    suspend fun loadSuggestedBots(dialogId: Long, sort: Int = 0): Result<List<StarRefProgramModel>>

    suspend fun loadAdminedBots(): Result<List<Long>>
    suspend fun loadAdminedChannels(): Result<List<Long>>
}
