package org.telegram.messenger.feature.secretchat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SecretChatHelper
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.secretchat.data.mapper.SecretChatMapper
import org.telegram.messenger.feature.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository
import org.telegram.tgnet.TLRPC

/**
 * Clean adapter implementing [SecretChatRepository] backed by legacy [SecretChatHelper] and [MessagesController].
 */
class LegacySecretChatRepository(
    private val currentAccount: Int
) : SecretChatRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val secretChatHelper: SecretChatHelper
        get() = SecretChatHelper.getInstance(currentAccount)

    private val messagesStorage: MessagesStorage
        get() = MessagesStorage.getInstance(currentAccount)

    private val currentUserId: Long
        get() = UserConfig.getInstance(currentAccount).clientUserId

    private fun mapChat(chat: TLRPC.EncryptedChat?): SecretChatModel? {
        if (chat == null) return null
        val user = messagesController.getUser(chat.user_id)
        return SecretChatMapper.toDomain(chat, user, currentUserId)
    }

    override fun observeSecretChat(chatId: Int): Flow<SecretChatModel?> = callbackFlow {
        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, args ->
            if (id == NotificationCenter.encryptedChatUpdated) {
                val updatedChat = args[0] as? TLRPC.EncryptedChat
                if (updatedChat != null && updatedChat.id == chatId) {
                    trySend(mapChat(updatedChat))
                }
            }
        }

        val nc = NotificationCenter.getInstance(currentAccount)
        nc.addObserver(delegate, NotificationCenter.encryptedChatUpdated)

        // Emit initial value
        val initialChat = messagesController.getEncryptedChat(chatId)
        trySend(mapChat(initialChat))

        awaitClose {
            nc.removeObserver(delegate, NotificationCenter.encryptedChatUpdated)
        }
    }.flowOn(Dispatchers.Main)

    override fun observeSecretChats(): Flow<List<SecretChatModel>> = callbackFlow {
        fun emitAll() {
            val list = mutableListOf<SecretChatModel>()
            val dialogs = messagesController.allDialogs
            for (i in 0 until dialogs.size) {
                val dialog = dialogs[i]
                if (DialogObject.isEncryptedDialog(dialog.id)) {
                    val chatId = dialog.id.toInt()
                    val chat = messagesController.getEncryptedChat(chatId)
                    val model = mapChat(chat)
                    if (model != null) {
                        list.add(model)
                    }
                }
            }
            trySend(list)
        }

        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            when (id) {
                NotificationCenter.encryptedChatUpdated,
                NotificationCenter.encryptedChatCreated,
                NotificationCenter.dialogsNeedReload -> emitAll()
            }
        }

        val nc = NotificationCenter.getInstance(currentAccount)
        nc.addObserver(delegate, NotificationCenter.encryptedChatUpdated)
        nc.addObserver(delegate, NotificationCenter.encryptedChatCreated)
        nc.addObserver(delegate, NotificationCenter.dialogsNeedReload)

        emitAll()

        awaitClose {
            nc.removeObserver(delegate, NotificationCenter.encryptedChatUpdated)
            nc.removeObserver(delegate, NotificationCenter.encryptedChatCreated)
            nc.removeObserver(delegate, NotificationCenter.dialogsNeedReload)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getSecretChat(chatId: Int): SecretChatModel? = withContext(Dispatchers.Main) {
        mapChat(messagesController.getEncryptedChat(chatId))
    }

    override suspend fun startSecretChat(userId: Long): Result<Int> = withContext(Dispatchers.Main) {
        val user = messagesController.getUser(userId)
        if (user == null) {
            Result.failure(AppError.NotFound("User with ID $userId not found"))
        } else {
            try {
                secretChatHelper.startSecretChat(ApplicationLoader.applicationContext, user)
                Result.success(0)
            } catch (e: Throwable) {
                Result.failure(AppError.Generic("Failed to start secret chat with user $userId", e))
            }
        }
    }

    override suspend fun acceptSecretChat(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = messagesController.getEncryptedChat(chatId)
        if (chat == null) {
            Result.failure(AppError.NotFound("Secret chat $chatId not found"))
        } else {
            try {
                secretChatHelper.acceptSecretChat(chat)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(AppError.Generic("Failed to accept secret chat $chatId", e))
            }
        }
    }

    override suspend fun declineSecretChat(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            secretChatHelper.declineSecretChat(chatId, true)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to decline secret chat $chatId", e))
        }
    }

    override suspend fun setTtl(chatId: Int, ttlSeconds: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = messagesController.getEncryptedChat(chatId)
        if (chat == null) {
            Result.failure(AppError.NotFound("Secret chat $chatId not found"))
        } else {
            try {
                chat.ttl = ttlSeconds
                secretChatHelper.sendTTLMessage(chat, null)
                messagesStorage.updateEncryptedChatTTL(chat)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(AppError.Generic("Failed to update TTL for secret chat $chatId", e))
            }
        }
    }

    override suspend fun sendScreenshotNotification(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = messagesController.getEncryptedChat(chatId)
        if (chat == null) {
            Result.failure(AppError.NotFound("Secret chat $chatId not found"))
        } else {
            try {
                secretChatHelper.sendScreenshotMessage(chat, null, null)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(AppError.Generic("Failed to send screenshot notification for secret chat $chatId", e))
            }
        }
    }
}
