package org.telegram.messenger.feature.messaging.savedmessages.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SavedMessagesController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.savedmessages.data.mapper.SavedMessagesMapper
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedTagModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Adapter implementing [SavedMessagesRepository] on top of legacy [SavedMessagesController].
 * Applies the Strangler Fig pattern: callers consume clean domain models and flows,
 * while legacy internals remain intact for upstream compatibility.
 */
class LegacySavedMessagesRepository(
    private val account: Int
) : SavedMessagesRepository {

    private val controller: SavedMessagesController
        get() = MessagesController.getInstance(account).savedMessagesController

    override fun observeSavedDialogs(): Flow<List<SavedDialogModel>> = callbackFlow {
        val emitCurrentState = {
            val list = controller.allDialogs.map { dialog ->
                SavedMessagesMapper.mapToDomain(account, dialog)
            }
            trySend(list)
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, _ ->
            if (id == NotificationCenter.savedMessagesDialogsUpdate && acc == account) {
                emitCurrentState()
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(
                observer,
                NotificationCenter.savedMessagesDialogsUpdate
            )
            // Emit current cached/loaded state immediately
            emitCurrentState()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(
                    observer,
                    NotificationCenter.savedMessagesDialogsUpdate
                )
            }
        }
    }

    override suspend fun getSavedDialogs(): Result<List<SavedDialogModel>> = withContext(Dispatchers.Main) {
        try {
            val dialogs = controller.allDialogs.map { dialog ->
                SavedMessagesMapper.mapToDomain(account, dialog)
            }
            Result.success(dialogs)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to read saved dialogs", e))
        }
    }

    override suspend fun loadDialogs(onlyCache: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            controller.loadDialogs(onlyCache)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to load saved dialogs", e))
        }
    }

    override suspend fun togglePin(dialogId: Long, pinned: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val dids = arrayListOf(dialogId)
            val success = controller.updatePinned(dids, pinned, true)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update pinned state or limit reached"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error toggling pin on dialog $dialogId", e))
        }
    }

    override suspend fun deleteDialog(dialogId: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            controller.deleteDialog(dialogId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Error deleting dialog $dialogId", e))
        }
    }

    override fun observeSavedTags(): Flow<List<SavedTagModel>> = callbackFlow {
        val emitTags = {
            val ms = MessagesController.getInstance(account)
            val tagsTL = ms.getSavedReactionTags(0L)
            val tags = if (tagsTL != null && tagsTL.tags != null) {
                tagsTL.tags.map { SavedMessagesMapper.mapTagToDomain(it) }
            } else {
                emptyList()
            }
            trySend(tags)
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, acc, _ ->
            if (id == NotificationCenter.savedReactionTagsUpdate && acc == account) {
                emitTags()
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            NotificationCenter.getInstance(account).addObserver(
                observer,
                NotificationCenter.savedReactionTagsUpdate
            )
            emitTags()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(account).removeObserver(
                    observer,
                    NotificationCenter.savedReactionTagsUpdate
                )
            }
        }
    }

    override fun searchDialogs(query: String): List<SavedDialogModel> {
        val results = controller.searchDialogs(query)
        return results.map { SavedMessagesMapper.mapToDomain(account, it) }
    }
}
