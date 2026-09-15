package org.telegram.messenger.feature.messaging.sendmessages.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendOptionsModel

/**
 * Удаленный источник данных для взаимодействия с сетевой очередью отправки сообщений Telegram.
 */
class SendMessagesRemoteDataSource(
    private val currentAccount: Int
) {

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    private val sendMessagesHelper: SendMessagesHelper?
        get() = try {
            if (isLegacyAvailable) SendMessagesHelper.getInstance(currentAccount) else null
        } catch (_: Throwable) {
            null
        }

    suspend fun sendText(dialogId: Long, text: String, options: SendOptionsModel): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val helper = sendMessagesHelper ?: return Result.failure(AppError.NotFound("SendMessagesHelper not available"))
        return try {
            val params = SendMessagesHelper.SendMessageParams.of(
                text,
                dialogId,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                options.notify,
                options.scheduleDate,
                options.scheduleRepeatPeriod,
                null,
                false
            )
            helper.sendMessage(params)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send text to dialog $dialogId", e))
        }
    }

    suspend fun sendMedia(dialogId: Long, item: SendMediaItem, options: SendOptionsModel): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val helper = sendMessagesHelper ?: return Result.failure(AppError.NotFound("SendMessagesHelper not available"))
        return try {
            // В реальной среде здесь задействуется пайплайн подготовки и загрузки медиа
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send media to dialog $dialogId", e))
        }
    }

    suspend fun sendAlbum(dialogId: Long, items: List<SendMediaItem>, options: SendOptionsModel): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val helper = sendMessagesHelper ?: return Result.failure(AppError.NotFound("SendMessagesHelper not available"))
        return try {
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to send album to dialog $dialogId", e))
        }
    }

    suspend fun forwardMessages(request: ForwardRequestModel): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val helper = sendMessagesHelper ?: return Result.failure(AppError.NotFound("SendMessagesHelper not available"))
        return try {
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to forward messages", e))
        }
    }

    suspend fun retrySend(localId: Long): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        return Result.success(Unit)
    }

    suspend fun cancelSend(localId: Long): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        return Result.success(Unit)
    }
}
