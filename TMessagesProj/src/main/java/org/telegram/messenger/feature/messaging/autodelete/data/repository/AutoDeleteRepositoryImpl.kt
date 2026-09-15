package org.telegram.messenger.feature.messaging.autodelete.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.autodelete.data.datasource.AutoDeleteLocalDataSource
import org.telegram.messenger.feature.messaging.autodelete.data.datasource.AutoDeleteRemoteDataSource
import org.telegram.messenger.feature.messaging.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.messaging.autodelete.domain.model.GlobalAutoDeleteStateModel
import org.telegram.messenger.feature.messaging.autodelete.domain.repository.AutoDeleteRepository

class AutoDeleteRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: AutoDeleteLocalDataSource,
    private val remoteDataSource: AutoDeleteRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : AutoDeleteRepository {

    override fun observeGlobalAutoDelete(): Flow<GlobalAutoDeleteStateModel> =
        localDataSource.globalAutoDeleteFlow

    override suspend fun getGlobalAutoDelete(forceRefresh: Boolean): Result<AutoDeleteTtlModel> =
        withContext(mainDispatcher) {
            if (!forceRefresh) {
                return@withContext Result.Success(localDataSource.getGlobalTtl())
            }

            localDataSource.setGlobalLoading(true)
            val remoteResult = remoteDataSource.getDefaultHistoryTTL()
            when (remoteResult) {
                is Result.Success -> {
                    localDataSource.setGlobalTtl(remoteResult.data)
                    Result.Success(AutoDeleteTtlModel(remoteResult.data))
                }
                is Result.Failure -> {
                    localDataSource.setGlobalLoading(false)
                    Result.Success(localDataSource.getGlobalTtl())
                }
            }
        }

    override suspend fun setGlobalAutoDelete(ttl: AutoDeleteTtlModel): Result<Unit> =
        withContext(mainDispatcher) {
            localDataSource.setGlobalLoading(true)
            val remoteResult = remoteDataSource.setDefaultHistoryTTL(ttl.periodSeconds)
            when (remoteResult) {
                is Result.Success -> {
                    localDataSource.setGlobalTtl(ttl.periodSeconds)
                    Result.Success(Unit)
                }
                is Result.Failure -> {
                    localDataSource.setGlobalLoading(false)
                    remoteResult
                }
            }
        }

    override suspend fun getChatAutoDelete(chatId: Long): Result<AutoDeleteTtlModel> =
        withContext(mainDispatcher) {
            Result.Success(localDataSource.getChatTtl(chatId))
        }

    override suspend fun setChatAutoDelete(chatId: Long, ttl: AutoDeleteTtlModel): Result<Unit> =
        withContext(mainDispatcher) {
            localDataSource.setChatTtl(chatId, ttl.periodSeconds)
            Result.Success(Unit)
        }

    override suspend fun setChatsAutoDeleteBatch(
        chatIds: List<Long>,
        ttl: AutoDeleteTtlModel
    ): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.setChatsAutoDeleteBatch(chatIds, ttl.periodSeconds)
        Result.Success(Unit)
    }
}
