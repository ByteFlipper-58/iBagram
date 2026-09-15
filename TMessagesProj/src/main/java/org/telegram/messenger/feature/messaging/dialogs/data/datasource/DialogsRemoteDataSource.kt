package org.telegram.messenger.feature.messaging.dialogs.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result

/**
 * Удаленный источник данных для взаимодействия с RPC и операциями над диалогами.
 */
class DialogsRemoteDataSource(
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

    suspend fun loadMoreDialogs(folderId: Int, offset: Int, count: Int): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            mc.loadDialogs(folderId, offset, count, false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to load more dialogs", e))
        }
    }

    suspend fun pinDialog(dialogId: Long, pin: Boolean): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            val success = mc.pinDialog(dialogId, pin, null, -1)
            if (success) Result.success(Unit) else Result.failure(AppError.Generic("Failed to pin dialog $dialogId"))
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Error pinning dialog", e))
        }
    }

    suspend fun deleteDialog(dialogId: Long, revoke: Boolean): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            mc.deleteDialog(dialogId, 0, revoke)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Error deleting dialog", e))
        }
    }

    suspend fun markAsRead(dialogId: Long): Result<Unit> {
        if (!isLegacyAvailable) return Result.success(Unit)
        val mc = messagesController ?: return Result.failure(AppError.NotFound("MessagesController not available"))
        return try {
            mc.markDialogAsRead(dialogId, Int.MAX_VALUE, 0, 0, false, 0, 0, true, 0)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Error marking dialog as read", e))
        }
    }
}
