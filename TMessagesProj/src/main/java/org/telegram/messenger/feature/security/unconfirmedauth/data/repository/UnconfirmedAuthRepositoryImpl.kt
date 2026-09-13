package org.telegram.messenger.feature.security.unconfirmedauth.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.data.datasource.UnconfirmedAuthLocalDataSource
import org.telegram.messenger.feature.security.unconfirmedauth.data.datasource.UnconfirmedAuthRemoteDataSource
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

/**
 * Modern repository implementation for Unconfirmed Authorizations, coordinating local and remote data sources.
 */
class UnconfirmedAuthRepositoryImpl(
    private val currentAccount: Int,
    private val remoteDataSource: UnconfirmedAuthRemoteDataSource,
    private val localDataSource: UnconfirmedAuthLocalDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : UnconfirmedAuthRepository {

    override fun observeUnconfirmedAuths(): Flow<UnconfirmedAuthStateModel> {
        return NotificationCenterFlowBridge.observeEvent(
            account = currentAccount,
            eventId = NotificationCenter.unconfirmedAuthUpdate
        )
            .map {
                localDataSource.getCachedAuths()
            }
            .onStart {
                emit(localDataSource.getCachedAuths())
            }
            .flowOn(mainDispatcher)
    }

    override suspend fun getUnconfirmedAuths(): UnconfirmedAuthStateModel =
        withContext(mainDispatcher) {
            localDataSource.getCachedAuths()
        }

    override suspend fun confirmAuth(hash: Long): Result<Boolean> =
        withContext(mainDispatcher) {
            val (legacyAuth, fallbackAuth) = localDataSource.findAuth(hash)
            val isBot = legacyAuth?.bot ?: fallbackAuth?.isBot ?: false
            val botId = legacyAuth?.bot_id ?: fallbackAuth?.botId ?: 0L

            val remoteResult = if (isBot) {
                remoteDataSource.confirmBotAuth(botId)
            } else {
                remoteDataSource.confirmUserAuth(hash)
            }

            when (remoteResult) {
                is Result.Success -> {
                    if (remoteResult.data) {
                        localDataSource.removeAuth(hash)
                        postNotificationSafely(NotificationCenter.unconfirmedAuthUpdate)
                    }
                    Result.Success(remoteResult.data)
                }
                is Result.Failure -> remoteResult
            }
        }

    override suspend fun denyAuth(hash: Long): Result<Boolean> =
        withContext(mainDispatcher) {
            val (legacyAuth, fallbackAuth) = localDataSource.findAuth(hash)
            val isBot = legacyAuth?.bot ?: fallbackAuth?.isBot ?: false
            val botId = legacyAuth?.bot_id ?: fallbackAuth?.botId ?: 0L

            val remoteResult = if (isBot) {
                remoteDataSource.denyBotAuth(botId)
            } else {
                remoteDataSource.denyUserAuth(hash)
            }

            when (remoteResult) {
                is Result.Success -> {
                    if (remoteResult.data) {
                        localDataSource.removeAuth(hash)
                        postNotificationSafely(NotificationCenter.unconfirmedAuthUpdate)
                    }
                    Result.Success(remoteResult.data)
                }
                is Result.Failure -> remoteResult
            }
        }

    override suspend fun confirmAll(): Result<Int> =
        withContext(mainDispatcher) {
            val current = localDataSource.getCachedAuths().auths
            if (current.isEmpty()) {
                return@withContext Result.Success(0)
            }

            var confirmedCount = 0
            val successfulHashes = mutableSetOf<Long>()
            for (auth in current) {
                val res = if (auth.isBot) {
                    remoteDataSource.confirmBotAuth(auth.botId ?: 0L)
                } else {
                    remoteDataSource.confirmUserAuth(auth.hash)
                }
                if (res is Result.Success && res.data) {
                    confirmedCount++
                    successfulHashes.add(auth.hash)
                }
            }

            if (successfulHashes.isNotEmpty()) {
                localDataSource.removeAuths(successfulHashes)
                postNotificationSafely(NotificationCenter.unconfirmedAuthUpdate)
            }

            Result.Success(confirmedCount)
        }

    override suspend fun denyAll(): Result<Int> =
        withContext(mainDispatcher) {
            val current = localDataSource.getCachedAuths().auths
            if (current.isEmpty()) {
                return@withContext Result.Success(0)
            }

            var deniedCount = 0
            val successfulHashes = mutableSetOf<Long>()
            for (auth in current) {
                val res = if (auth.isBot) {
                    remoteDataSource.denyBotAuth(auth.botId ?: 0L)
                } else {
                    remoteDataSource.denyUserAuth(auth.hash)
                }
                if (res is Result.Success && res.data) {
                    deniedCount++
                    successfulHashes.add(auth.hash)
                }
            }

            if (successfulHashes.isNotEmpty()) {
                localDataSource.removeAuths(successfulHashes)
                postNotificationSafely(NotificationCenter.unconfirmedAuthUpdate)
            }

            Result.Success(deniedCount)
        }

    override suspend fun clear(): Result<Unit> =
        withContext(mainDispatcher) {
            localDataSource.clearAll()
            postNotificationSafely(NotificationCenter.unconfirmedAuthUpdate)
            Result.Success(Unit)
        }

    private fun postNotificationSafely(id: Int) {
        try {
            NotificationCenter.getInstance(currentAccount)?.postNotificationName(id)
        } catch (_: Throwable) {
            // Ignored in headless unit test environment
        }
    }
}
