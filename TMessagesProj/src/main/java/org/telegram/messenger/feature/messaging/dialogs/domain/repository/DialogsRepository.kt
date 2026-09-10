package org.telegram.messenger.feature.messaging.dialogs.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel

/**
 * Domain boundary contract for managing the chat dialogs list.
 */
interface DialogsRepository {
    /**
     * Observe the reactive stream of dialogs for a specific folder.
     * folderId = 0 is the default/main folder, 1 is the archive folder.
     */
    fun getDialogs(folderId: Int = 0): Flow<List<DialogModel>>

    /**
     * Request loading additional older dialogs for pagination.
     */
    suspend fun loadMoreDialogs(folderId: Int = 0, count: Int = 30): Result<Unit>

    /**
     * Pin or unpin a dialog.
     */
    suspend fun pinDialog(dialogId: Long, pin: Boolean): Result<Unit>

    /**
     * Delete a dialog. If revoke is true, deletes for both sides when applicable.
     */
    suspend fun deleteDialog(dialogId: Long, revoke: Boolean = true): Result<Unit>

    /**
     * Mark all unread messages in a dialog as read.
     */
    suspend fun markAsRead(dialogId: Long): Result<Unit>
}
