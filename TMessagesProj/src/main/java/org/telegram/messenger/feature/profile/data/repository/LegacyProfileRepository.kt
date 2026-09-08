package org.telegram.messenger.feature.profile.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.profile.data.mapper.ProfileMapper
import org.telegram.messenger.feature.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.profile.domain.repository.ProfileRepository

/**
 * Adapter implementing [ProfileRepository] by delegating to legacy [MessagesController].
 * Reads are strictly confined to [Dispatchers.Main] to prevent threading races on internal caches.
 */
class LegacyProfileRepository(
    private val account: Int
) : ProfileRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(account)

    private fun readProfileFromLegacy(id: Long): ProfileModel? {
        val isBlocked = messagesController.blockePeers.indexOfKey(id) >= 0

        return if (id > 0) {
            val user = messagesController.getUser(id) ?: return null
            val userFull = messagesController.getUserFull(id)
            ProfileMapper.mapToDomain(user, userFull, isBlocked)
        } else {
            val chatId = -id
            val chat = messagesController.getChat(chatId) ?: return null
            val chatFull = messagesController.getChatFull(chatId)
            ProfileMapper.mapToDomain(chat, chatFull, isBlocked)
        }
    }

    override fun observeProfile(id: Long): Flow<ProfileModel?> = callbackFlow {
        val emitCurrentState = {
            val profile = readProfileFromLegacy(id)
            trySend(profile)
        }

        val observer = NotificationCenter.NotificationCenterDelegate { notifId, acc, _ ->
            if (acc == account) {
                when (notifId) {
                    NotificationCenter.userInfoDidLoad,
                    NotificationCenter.chatInfoDidLoad,
                    NotificationCenter.blockedUsersDidLoad,
                    NotificationCenter.updateInterfaces -> {
                        emitCurrentState()
                    }
                }
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.userInfoDidLoad)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.chatInfoDidLoad)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.blockedUsersDidLoad)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.updateInterfaces)
            emitCurrentState()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.userInfoDidLoad)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.chatInfoDidLoad)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.blockedUsersDidLoad)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.updateInterfaces)
            }
        }
    }

    override suspend fun getProfile(id: Long): Result<ProfileModel> = withContext(Dispatchers.Main) {
        try {
            val profile = readProfileFromLegacy(id)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(AppError.NotFound("Profile for id $id not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error getting profile for id $id", e))
        }
    }

    override suspend fun loadFullProfile(id: Long): Result<ProfileModel> = withContext(Dispatchers.Main) {
        try {
            if (id > 0) {
                val user = messagesController.getUser(id)
                if (user != null) {
                    messagesController.loadFullUser(user, 0, true)
                }
            } else {
                val chatId = -id
                messagesController.loadFullChat(chatId, 0, true)
            }
            val profile = readProfileFromLegacy(id)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(AppError.NotFound("Profile for id $id not found after loading full info"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error loading full profile for id $id", e))
        }
    }

    override suspend fun blockPeer(id: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesController.blockPeer(id)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to block peer $id", e))
        }
    }

    override suspend fun unblockPeer(id: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesController.unblockPeer(id)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to unblock peer $id", e))
        }
    }
}
