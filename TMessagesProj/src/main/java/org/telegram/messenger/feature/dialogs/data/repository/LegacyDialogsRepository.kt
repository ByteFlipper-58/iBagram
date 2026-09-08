package org.telegram.messenger.feature.dialogs.data.repository

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
import org.telegram.messenger.feature.dialogs.data.mapper.DialogMapper
import org.telegram.messenger.feature.dialogs.domain.model.DialogModel
import org.telegram.messenger.feature.dialogs.domain.repository.DialogsRepository

/**
 * Adapter implementing [DialogsRepository] by wrapping [MessagesController]
 * and observing [NotificationCenter] events.
 *
 * Ensures all accesses to in-memory collections of [MessagesController] occur
 * on [Dispatchers.Main] to uphold the Telegram threading contract and avoid
 * ConcurrentModificationException.
 */
class LegacyDialogsRepository(
    private val account: Int
) : DialogsRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(account)

    override fun getDialogs(folderId: Int): Flow<List<DialogModel>> = callbackFlow {
        val emitCurrentState = {
            val legacyDialogs = messagesController.getDialogs(folderId)
            val list = legacyDialogs.map { dialog ->
                val isMuted = messagesController.isDialogMuted(dialog.id, 0)
                val isForum = messagesController.isForum(dialog.id)
                DialogMapper.mapToDomain(dialog, isMuted = isMuted, isForum = isForum)
            }
            trySend(list)
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, _ ->
            if (acc == account && (
                id == NotificationCenter.dialogsNeedReload ||
                id == NotificationCenter.updateInterfaces ||
                id == NotificationCenter.dialogDeleted
            )) {
                emitCurrentState()
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.dialogsNeedReload)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.updateInterfaces)
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.dialogDeleted)
            emitCurrentState()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.dialogsNeedReload)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.updateInterfaces)
                NotificationCenter.getInstance(account).removeObserver(observer, NotificationCenter.dialogDeleted)
            }
        }
    }

    override suspend fun loadMoreDialogs(folderId: Int, count: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val currentList = messagesController.getDialogs(folderId)
            messagesController.loadDialogs(folderId, currentList.size, count, false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load more dialogs for folder $folderId", e))
        }
    }

    override suspend fun pinDialog(dialogId: Long, pin: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val success = messagesController.pinDialog(dialogId, pin, null, -1)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to pin dialog $dialogId or pin limit reached"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error pinning dialog $dialogId", e))
        }
    }

    override suspend fun deleteDialog(dialogId: Long, revoke: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesController.deleteDialog(dialogId, 0, revoke)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error deleting dialog $dialogId", e))
        }
    }

    override suspend fun markAsRead(dialogId: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesController.markDialogAsRead(dialogId, Int.MAX_VALUE, 0, 0, false, 0, 0, true, 0)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error marking dialog $dialogId as read", e))
        }
    }
}
