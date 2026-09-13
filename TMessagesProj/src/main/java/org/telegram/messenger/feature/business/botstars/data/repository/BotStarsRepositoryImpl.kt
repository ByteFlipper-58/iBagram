package org.telegram.messenger.feature.business.botstars.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.data.datasource.BotStarsLocalDataSource
import org.telegram.messenger.feature.business.botstars.data.datasource.BotStarsRemoteDataSource
import org.telegram.messenger.feature.business.botstars.data.mapper.BotStarsMapper
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.business.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.business.botstars.domain.model.StarRefProgramModel
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository

/**
 * Repository implementation coordinating BotStarsLocalDataSource and BotStarsRemoteDataSource.
 */
class BotStarsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BotStarsLocalDataSource,
    private val remoteDataSource: BotStarsRemoteDataSource,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BotStarsRepository {

    override fun observeBotStarsStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> {
        return localDataSource.observeBotStarsUpdated()
            .map { updatedDid ->
                if (updatedDid == null || updatedDid == dialogId) {
                    val stats = localDataSource.getStarsRevenueStats(dialogId)
                    BotStarsMapper.toRevenueStats(dialogId, stats)
                } else {
                    val stats = localDataSource.getStarsRevenueStats(dialogId)
                    BotStarsMapper.toRevenueStats(dialogId, stats)
                }
            }
            .onStart {
                val cached = localDataSource.getStarsRevenueStats(dialogId)
                emit(BotStarsMapper.toRevenueStats(dialogId, cached))
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun getBotStarsStats(
        dialogId: Long,
        force: Boolean
    ): Result<BotStarsRevenueStatsModel?> = withContext(defaultDispatcher) {
        val cached = localDataSource.getStarsRevenueStats(dialogId)
        if (!force && cached != null) {
            return@withContext Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
        }

        when (val res = remoteDataSource.fetchBotStarsStats(dialogId, force)) {
            is Result.Success -> {
                res.data?.let { localDataSource.saveStarsRevenueStats(dialogId, it) }
                Result.Success(BotStarsMapper.toRevenueStats(dialogId, res.data))
            }
            is Result.Failure -> {
                if (cached != null) {
                    Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override fun observeTonStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> {
        return localDataSource.observeBotStarsUpdated()
            .map { updatedDid ->
                if (updatedDid == null || updatedDid == dialogId) {
                    val stats = localDataSource.getTonRevenueStats(dialogId)
                    BotStarsMapper.toRevenueStats(dialogId, stats)
                } else {
                    val stats = localDataSource.getTonRevenueStats(dialogId)
                    BotStarsMapper.toRevenueStats(dialogId, stats)
                }
            }
            .onStart {
                val cached = localDataSource.getTonRevenueStats(dialogId)
                emit(BotStarsMapper.toRevenueStats(dialogId, cached))
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun getTonStats(
        dialogId: Long,
        force: Boolean
    ): Result<BotStarsRevenueStatsModel?> = withContext(defaultDispatcher) {
        val cached = localDataSource.getTonRevenueStats(dialogId)
        if (!force && cached != null) {
            return@withContext Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
        }

        when (val res = remoteDataSource.fetchTonStats(dialogId, force)) {
            is Result.Success -> {
                res.data?.let { localDataSource.saveTonRevenueStats(dialogId, it) }
                Result.Success(BotStarsMapper.toRevenueStats(dialogId, res.data))
            }
            is Result.Failure -> {
                if (cached != null) {
                    Result.Success(BotStarsMapper.toRevenueStats(dialogId, cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override fun observeTransactions(
        dialogId: Long,
        type: BotStarsTransactionType
    ): Flow<List<BotStarsTransactionModel>> {
        return localDataSource.observeTransactionsLoaded()
            .map { updatedDid ->
                val txs = localDataSource.getTransactions(dialogId, type.legacyId)
                BotStarsMapper.toTransactionList(txs)
            }
            .onStart {
                val currentTxs = localDataSource.getTransactions(dialogId, type.legacyId)
                emit(BotStarsMapper.toTransactionList(currentTxs))
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun loadTransactions(
        dialogId: Long,
        type: BotStarsTransactionType,
        reload: Boolean
    ): Result<List<BotStarsTransactionModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getTransactions(dialogId, type.legacyId)
        if (!reload && cached.isNotEmpty()) {
            return@withContext Result.Success(BotStarsMapper.toTransactionList(cached))
        }

        when (val res = remoteDataSource.fetchTransactions(dialogId, type.legacyId, reload)) {
            is Result.Success -> {
                localDataSource.saveTransactions(dialogId, type.legacyId, res.data)
                Result.Success(BotStarsMapper.toTransactionList(res.data))
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(BotStarsMapper.toTransactionList(cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override fun observeConnectedBots(dialogId: Long): Flow<List<ConnectedBotStarRefModel>> {
        return localDataSource.observeConnectedBotsUpdate()
            .map {
                val channelBots = localDataSource.getConnectedBots(dialogId)
                BotStarsMapper.toConnectedBotList(channelBots)
            }
            .onStart {
                val channelBots = localDataSource.getConnectedBots(dialogId)
                emit(BotStarsMapper.toConnectedBotList(channelBots))
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun loadConnectedBots(
        dialogId: Long,
        reload: Boolean
    ): Result<List<ConnectedBotStarRefModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getConnectedBots(dialogId)
        if (!reload && cached.isNotEmpty()) {
            return@withContext Result.Success(BotStarsMapper.toConnectedBotList(cached))
        }

        when (val res = remoteDataSource.fetchConnectedBots(dialogId, reload)) {
            is Result.Success -> {
                localDataSource.saveConnectedBots(dialogId, res.data)
                Result.Success(BotStarsMapper.toConnectedBotList(res.data))
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(BotStarsMapper.toConnectedBotList(cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun loadSuggestedBots(
        dialogId: Long,
        sort: Int
    ): Result<List<StarRefProgramModel>> = withContext(defaultDispatcher) {
        when (val res = remoteDataSource.fetchSuggestedBots(dialogId, sort)) {
            is Result.Success -> {
                localDataSource.saveSuggestedBots(dialogId, res.data)
                Result.Success(BotStarsMapper.toSuggestedBotList(res.data))
            }
            is Result.Failure -> {
                val cached = localDataSource.getSuggestedBots(dialogId)
                if (cached.isNotEmpty()) {
                    Result.Success(BotStarsMapper.toSuggestedBotList(cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun loadAdminedBots(): Result<List<Long>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getAdminedBots()
        if (cached.isNotEmpty()) {
            return@withContext Result.Success(cached)
        }

        when (val res = remoteDataSource.fetchAdminedBots()) {
            is Result.Success -> {
                localDataSource.saveAdminedBots(res.data)
                Result.Success(res.data)
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun loadAdminedChannels(): Result<List<Long>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getAdminedChannels()
        if (cached.isNotEmpty()) {
            return@withContext Result.Success(cached)
        }

        when (val res = remoteDataSource.fetchAdminedChannels()) {
            is Result.Success -> {
                localDataSource.saveAdminedChannels(res.data)
                Result.Success(res.data)
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }
}
