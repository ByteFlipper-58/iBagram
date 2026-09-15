package org.telegram.messenger.feature.messaging.chat.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result

/**
 * Удаленный источник данных для загрузки истории сообщений, отправки и удаления через MTProto.
 */
class ChatRemoteDataSource(
    private val currentAccount: Int
) {

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    private val messagesController: MessagesController?
        get() = try {
            if (isLegacyAvailable) MessagesController.getInstance(currentAccount) else null
        } catch (_: Throwable) {
            null
        }

    private val sendMessagesHelper: SendMessagesHelper?
        get() = try {
            if (isLegacyAvailable) SendMessagesHelper.getInstance(currentAccount) else null
        } catch (_: Throwable) {
            null
        }

    suspend fun loadHistory(dialogId: Long, count: Int): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            mc.loadMessages(dialogId, 0, false, count, 0, 0, true, 0, 0, 0, 0, 0, 0, 0, 0, false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load history for dialog $dialogId", e))
        }
    }

    suspend fun sendMessage(dialogId: Long, text: String): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val smh = sendMessagesHelper ?: return Result.failure(AppError.NotFound("SendMessagesHelper not available"))
        return try {
            val params = SendMessagesHelper.SendMessageParams.of(text, dialogId)
            smh.sendMessage(params)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send message to dialog $dialogId", e))
        }
    }

    suspend fun deleteMessages(dialogId: Long, messageIds: List<Int>, revoke: Boolean): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            val ids = ArrayList<Int>(messageIds)
            mc.deleteMessages(ids, null, null, dialogId, 0, revoke, 0)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to delete messages in dialog $dialogId", e))
        }
    }
}
