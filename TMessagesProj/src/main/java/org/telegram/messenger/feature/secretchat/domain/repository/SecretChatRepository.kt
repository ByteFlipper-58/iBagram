package org.telegram.messenger.feature.secretchat.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.secretchat.domain.model.SecretChatModel

/**
 * Clean domain repository contract for managing end-to-end encrypted secret chats.
 */
interface SecretChatRepository {
    fun observeSecretChat(chatId: Int): Flow<SecretChatModel?>
    fun observeSecretChats(): Flow<List<SecretChatModel>>
    suspend fun getSecretChat(chatId: Int): SecretChatModel?
    suspend fun startSecretChat(userId: Long): Result<Int>
    suspend fun acceptSecretChat(chatId: Int): Result<Unit>
    suspend fun declineSecretChat(chatId: Int): Result<Unit>
    suspend fun setTtl(chatId: Int, ttlSeconds: Int): Result<Unit>
    suspend fun sendScreenshotNotification(chatId: Int): Result<Unit>
}
