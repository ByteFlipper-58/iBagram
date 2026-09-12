package org.telegram.messenger.feature.security.secretchat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatLocalDataSource
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatRemoteDataSource
import org.telegram.messenger.feature.security.secretchat.data.mapper.SecretChatMapper
import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.security.secretchat.domain.repository.SecretChatRepository
import org.telegram.tgnet.TLRPC

/**
 * Clean domain repository implementation coordinating local SQLite/cache data source and
 * remote MTProto encrypted chat requests.
 */
class SecretChatRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: SecretChatLocalDataSource,
    private val remoteDataSource: SecretChatRemoteDataSource
) : SecretChatRepository {

    private fun mapChat(chat: TLRPC.EncryptedChat?): SecretChatModel? {
        if (chat == null) return null
        val user = localDataSource.getUser(chat.user_id)
        val currentUserId = localDataSource.getClientUserId()
        return SecretChatMapper.toDomain(chat, user, currentUserId)
    }

    override fun observeSecretChat(chatId: Int): Flow<SecretChatModel?> = callbackFlow {
        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, args ->
            if (id == NotificationCenter.encryptedChatUpdated) {
                val updatedChat = args?.getOrNull(0) as? TLRPC.EncryptedChat
                if (updatedChat != null && updatedChat.id == chatId) {
                    trySend(mapChat(updatedChat))
                }
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            try {
                NotificationCenter.getInstance(currentAccount)?.addObserver(delegate, NotificationCenter.encryptedChatUpdated)
            } catch (_: Throwable) {
                // Handled gracefully in headless or non-Android environments
            }
        }

        // Emit current cached state
        val currentChat = localDataSource.getEncryptedChat(chatId)
        trySend(mapChat(currentChat))

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                try {
                    NotificationCenter.getInstance(currentAccount)?.removeObserver(delegate, NotificationCenter.encryptedChatUpdated)
                } catch (_: Throwable) {
                    // Handled gracefully
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun observeSecretChats(): Flow<List<SecretChatModel>> = callbackFlow {
        fun emitAll() {
            val list = mutableListOf<SecretChatModel>()
            val dialogs = localDataSource.getAllDialogs()
            for (dialog in dialogs) {
                if (isEncryptedDialog(dialog.id)) {
                    val chatId = getEncryptedChatId(dialog.id)
                    val chat = localDataSource.getEncryptedChat(chatId)
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

        NotificationCenterFlowBridge.runOnMainThread {
            try {
                val nc = NotificationCenter.getInstance(currentAccount)
                nc?.addObserver(delegate, NotificationCenter.encryptedChatUpdated)
                nc?.addObserver(delegate, NotificationCenter.encryptedChatCreated)
                nc?.addObserver(delegate, NotificationCenter.dialogsNeedReload)
            } catch (_: Throwable) {
                // Handled gracefully in headless or non-Android environments
            }
        }

        emitAll()

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                try {
                    val nc = NotificationCenter.getInstance(currentAccount)
                    nc?.removeObserver(delegate, NotificationCenter.encryptedChatUpdated)
                    nc?.removeObserver(delegate, NotificationCenter.encryptedChatCreated)
                    nc?.removeObserver(delegate, NotificationCenter.dialogsNeedReload)
                } catch (_: Throwable) {
                    // Handled gracefully
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSecretChat(chatId: Int): SecretChatModel? = withContext(Dispatchers.IO) {
        mapChat(localDataSource.getEncryptedChat(chatId))
    }

    override suspend fun startSecretChat(userId: Long): Result<Int> = withContext(Dispatchers.Main) {
        val user = localDataSource.getUser(userId)
            ?: return@withContext Result.failure(AppError.NotFound("User with ID $userId not found"))

        if (localDataSource.isFrozen()) {
            return@withContext Result.failure(AppError.Generic("Account is frozen"))
        }

        try {
            val context = try {
                ApplicationLoader.applicationContext
            } catch (_: Throwable) {
                null
            }
            localDataSource.startSecretChat(context, user)
            Result.success(0)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to start secret chat with user $userId", e))
        }
    }

    override suspend fun acceptSecretChat(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = localDataSource.getEncryptedChat(chatId)
            ?: return@withContext Result.failure(AppError.NotFound("Secret chat $chatId not found"))

        try {
            localDataSource.acceptSecretChat(chat)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to accept secret chat $chatId", e))
        }
    }

    override suspend fun declineSecretChat(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            localDataSource.declineSecretChat(chatId, revoke = true)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to decline secret chat $chatId", e))
        }
    }

    override suspend fun setTtl(chatId: Int, ttlSeconds: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = localDataSource.getEncryptedChat(chatId)
            ?: return@withContext Result.failure(AppError.NotFound("Secret chat $chatId not found"))

        try {
            chat.ttl = ttlSeconds
            localDataSource.sendTTLMessage(chat)
            localDataSource.updateEncryptedChatTTL(chat)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to update TTL for secret chat $chatId", e))
        }
    }

    override suspend fun sendScreenshotNotification(chatId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val chat = localDataSource.getEncryptedChat(chatId)
            ?: return@withContext Result.failure(AppError.NotFound("Secret chat $chatId not found"))

        try {
            localDataSource.sendScreenshotMessage(chat)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send screenshot notification for secret chat $chatId", e))
        }
    }

    companion object {
        fun isEncryptedDialog(dialogId: Long): Boolean =
            (dialogId and 0x4000000000000000L) != 0L && (dialogId and Long.MIN_VALUE) == 0L

        fun makeEncryptedDialogId(chatId: Long): Long =
            0x4000000000000000L or (chatId and 0xffffffffL)

        fun getEncryptedChatId(dialogId: Long): Int =
            (dialogId and 0xffffffffL).toInt()
    }
}
