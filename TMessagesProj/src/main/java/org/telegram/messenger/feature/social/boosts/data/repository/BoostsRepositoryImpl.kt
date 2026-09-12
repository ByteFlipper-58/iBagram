package org.telegram.messenger.feature.social.boosts.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.data.datasource.BoostsLocalDataSource
import org.telegram.messenger.feature.social.boosts.data.datasource.BoostsRemoteDataSource
import org.telegram.messenger.feature.social.boosts.data.mapper.BoostMapper
import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository

/**
 * Clean repository implementation coordinating local and remote data sources for boosts operations,
 * strangling legacy monolithic logic in ChannelBoostsController.
 */
class BoostsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BoostsLocalDataSource,
    private val remoteDataSource: BoostsRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BoostsRepository {

    override suspend fun getBoostsStatus(dialogId: Long): Result<BoostStatusModel> = withContext(ioDispatcher) {
        val normalizedId = if (dialogId < 0) dialogId else -dialogId
        val inputPeer = localDataSource.getInputPeer(normalizedId)
            ?: return@withContext Result.failure("Failed to resolve input peer for dialogId $dialogId")

        when (val remoteResult = remoteDataSource.getBoostsStatus(inputPeer)) {
            is Result.Success -> Result.Success(BoostMapper.mapToBoostStatus(remoteResult.data))
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }

    override suspend fun getMyBoosts(): Result<MyBoostsModel> = withContext(ioDispatcher) {
        when (val remoteResult = remoteDataSource.getMyBoosts()) {
            is Result.Success -> {
                localDataSource.putUsersAndChats(remoteResult.data.users, remoteResult.data.chats)
                Result.Success(BoostMapper.mapToMyBoosts(remoteResult.data))
            }
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }

    override suspend fun checkCanApplyBoost(dialogId: Long): Result<CanApplyBoostModel> = withContext(ioDispatcher) {
        val normalizedId = if (dialogId < 0) dialogId else -dialogId
        val inputPeer = localDataSource.getInputPeer(normalizedId)
            ?: return@withContext Result.failure("Failed to resolve input peer for dialogId $dialogId")

        val statusResult = remoteDataSource.getBoostsStatus(inputPeer)
        val status = when (statusResult) {
            is Result.Success -> statusResult.data
            is Result.Failure -> return@withContext Result.Failure(statusResult.error)
        }

        val canApply = localDataSource.checkCanApplyBoost(normalizedId, status)
        if (canApply != null) {
            Result.Success(BoostMapper.mapToCanApplyBoost(canApply))
        } else {
            Result.Failure(AppError.Generic("Failed to determine boost eligibility"))
        }
    }

    override suspend fun applyBoost(dialogId: Long, slots: List<Int>): Result<MyBoostsModel> = withContext(ioDispatcher) {
        val normalizedId = if (dialogId < 0) dialogId else -dialogId
        val inputPeer = localDataSource.getInputPeer(normalizedId)
            ?: return@withContext Result.failure("Failed to resolve input peer for dialogId $dialogId")

        when (val remoteResult = remoteDataSource.applyBoost(inputPeer, slots)) {
            is Result.Success -> {
                localDataSource.putUsersAndChats(remoteResult.data.users, remoteResult.data.chats)
                Result.Success(BoostMapper.mapToMyBoosts(remoteResult.data))
            }
            is Result.Failure -> Result.Failure(remoteResult.error)
        }
    }
}
