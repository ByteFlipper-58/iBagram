package org.telegram.messenger.feature.business.businessbots.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.data.datasource.BusinessBotsLocalDataSource
import org.telegram.messenger.feature.business.businessbots.data.datasource.BusinessBotsRemoteDataSource
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository

class BusinessBotsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BusinessBotsLocalDataSource,
    private val remoteDataSource: BusinessBotsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BusinessBotsRepository {

    override fun observeConnectedBots(): Flow<List<ConnectedBotModel>> {
        return localDataSource.observeUpdatedChatbot()
            .map { localDataSource.getConnectedBots() }
            .onStart { emit(localDataSource.getConnectedBots()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getConnectedBots(): List<ConnectedBotModel> = withContext(mainDispatcher) {
        localDataSource.getConnectedBots()
    }

    override suspend fun loadConnectedBots(forceReload: Boolean): Result<List<ConnectedBotModel>> = withContext(mainDispatcher) {
        if (forceReload) {
            localDataSource.invalidate(false)
        }
        when (val res = remoteDataSource.getConnectedBots()) {
            is Result.Success -> {
                localDataSource.setTestBots(res.data)
                Result.Success(res.data)
            }
            is Result.Failure -> {
                // Fallback to local cached bots
                val cached = localDataSource.getConnectedBots()
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun updateConnectedBot(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ): Result<ConnectedBotModel> = withContext(mainDispatcher) {
        when (val res = remoteDataSource.updateConnectedBot(botId, rights, recipients)) {
            is Result.Success -> {
                localDataSource.updateBot(res.data)
                localDataSource.invalidate(true)
                Result.Success(res.data)
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override suspend fun deleteConnectedBot(botId: Long): Result<Unit> = withContext(mainDispatcher) {
        when (val res = remoteDataSource.deleteConnectedBot(botId)) {
            is Result.Success -> {
                localDataSource.deleteBot(botId)
                localDataSource.invalidate(true)
                Result.Success(Unit)
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override fun findConnectedBot(botId: Long): ConnectedBotModel? {
        return localDataSource.findConnectedBot(botId)
    }
}
