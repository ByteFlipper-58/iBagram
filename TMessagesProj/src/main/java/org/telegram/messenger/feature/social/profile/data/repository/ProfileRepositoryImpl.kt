package org.telegram.messenger.feature.social.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.data.datasource.ProfileLocalDataSource
import org.telegram.messenger.feature.social.profile.data.datasource.ProfileRemoteDataSource
import org.telegram.messenger.feature.social.profile.data.mapper.ProfileMapper
import org.telegram.messenger.feature.social.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository

/**
 * Modern repository implementation managing profile caching and remote loading.
 */
class ProfileRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: ProfileLocalDataSource,
    private val remoteDataSource: ProfileRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ProfileRepository {

    private fun readProfile(id: Long): ProfileModel? {
        val isBlocked = localDataSource.isBlocked(id)
        return if (id > 0) {
            val user = localDataSource.getUser(id) ?: return null
            val userFull = localDataSource.getUserFull(id)
            ProfileMapper.mapToDomain(user, userFull, isBlocked)
        } else {
            val chatId = -id
            val chat = localDataSource.getChat(chatId) ?: return null
            val chatFull = localDataSource.getChatFull(chatId)
            ProfileMapper.mapToDomain(chat, chatFull, isBlocked)
        }
    }

    override fun observeProfile(id: Long): Flow<ProfileModel?> {
        return localDataSource.observeProfileEvents()
            .map { readProfile(id) }
            .onStart { emit(readProfile(id)) }
    }

    override suspend fun getProfile(id: Long): Result<ProfileModel> = withContext(mainDispatcher) {
        try {
            val profile = readProfile(id)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(AppError.NotFound("Profile for id $id not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error getting profile for id $id", e))
        }
    }

    override suspend fun loadFullProfile(id: Long): Result<ProfileModel> = withContext(mainDispatcher) {
        try {
            if (id > 0) {
                val user = localDataSource.getUser(id)
                if (user != null) {
                    remoteDataSource.loadFullUser(user)
                }
            } else {
                val chatId = -id
                remoteDataSource.loadFullChat(chatId)
            }
            val profile = readProfile(id)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(AppError.NotFound("Profile for id $id not found after loading full info"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error loading full profile for id $id", e))
        }
    }

    override suspend fun blockPeer(id: Long): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.setBlocked(id, true)
        remoteDataSource.blockPeer(id)
    }

    override suspend fun unblockPeer(id: Long): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.setBlocked(id, false)
        remoteDataSource.unblockPeer(id)
    }
}
