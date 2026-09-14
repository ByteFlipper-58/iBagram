package org.telegram.messenger.feature.media.voip.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.voip.data.datasource.VoIPLocalDataSource
import org.telegram.messenger.feature.media.voip.data.datasource.VoIPRemoteDataSource
import org.telegram.messenger.feature.media.voip.domain.model.CallModel
import org.telegram.messenger.feature.media.voip.domain.repository.VoIPRepository

class VoIPRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: VoIPLocalDataSource,
    private val remoteDataSource: VoIPRemoteDataSource
) : VoIPRepository {

    override fun observeCurrentCall(): Flow<CallModel?> = localDataSource.currentCall

    override suspend fun getCurrentCall(): CallModel? = localDataSource.getCurrentCall()

    override suspend fun startCall(userId: Long, isVideo: Boolean): Result<Unit> {
        localDataSource.startCall(userId, isVideo)
        remoteDataSource.initiateCall(userId, isVideo)
        return Result.Success(Unit)
    }

    override suspend fun acceptCall(): Result<Unit> {
        localDataSource.acceptCall()
        remoteDataSource.acceptCall()
        return Result.Success(Unit)
    }

    override suspend fun declineCall(): Result<Unit> {
        localDataSource.declineCall()
        remoteDataSource.declineCall()
        return Result.Success(Unit)
    }

    override suspend fun hangUp(): Result<Unit> {
        localDataSource.hangUp()
        remoteDataSource.hangUp()
        return Result.Success(Unit)
    }

    override suspend fun toggleMute(): Result<Boolean> {
        val muted = localDataSource.toggleMute()
        return Result.Success(muted)
    }

    override suspend fun toggleSpeakerphone(): Result<Boolean> {
        val speaker = localDataSource.toggleSpeakerphone()
        return Result.Success(speaker)
    }
}
