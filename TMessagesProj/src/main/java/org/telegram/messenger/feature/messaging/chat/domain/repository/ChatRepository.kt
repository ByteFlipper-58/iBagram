package org.telegram.messenger.feature.messaging.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel

/**
 * Domain boundary contract for managing chat messages and sending operations.
 */
interface ChatRepository {
    /**
     * Observe the reactive stream of messages for a specific dialog.
     */
    fun observeMessages(dialogId: Long): Flow<List<MessageModel>>

    /**
     * Get currently available messages for a dialog.
     */
    suspend fun getMessages(dialogId: Long): Result<List<MessageModel>>

    /**
     * Load older messages from storage or network.
     */
    suspend fun loadHistory(dialogId: Long, count: Int = 30): Result<Unit>

    /**
     * Send a plain text message to the specified dialog.
     */
    suspend fun sendMessage(dialogId: Long, text: String): Result<Unit>

    /**
     * Delete messages in a dialog. If revoke is true, deletes for all participants if allowed.
     */
    suspend fun deleteMessages(dialogId: Long, messageIds: List<Int>, revoke: Boolean = true): Result<Unit>
}
