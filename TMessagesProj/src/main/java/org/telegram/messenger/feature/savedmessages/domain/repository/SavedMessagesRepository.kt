package org.telegram.messenger.feature.savedmessages.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.model.SavedTagModel

/**
 * Domain repository contract defining data operations for Saved Messages.
 */
interface SavedMessagesRepository {
    /**
     * Emits the current list of saved dialogs reactively whenever updates occur.
     */
    fun observeSavedDialogs(): Flow<List<SavedDialogModel>>

    /**
     * Gets the snapshot of currently available saved dialogs.
     */
    suspend fun getSavedDialogs(): Result<List<SavedDialogModel>>

    /**
     * Triggers loading of saved dialogs from cache or network.
     */
    suspend fun loadDialogs(onlyCache: Boolean = false): Result<Unit>

    /**
     * Pins or unpins a specific dialog in Saved Messages.
     */
    suspend fun togglePin(dialogId: Long, pinned: Boolean): Result<Unit>

    /**
     * Deletes a saved dialog.
     */
    suspend fun deleteDialog(dialogId: Long): Result<Unit>

    /**
     * Observes saved reaction tags.
     */
    fun observeSavedTags(): Flow<List<SavedTagModel>>

    /**
     * Synchronously searches cached dialogs by query text.
     */
    fun searchDialogs(query: String): List<SavedDialogModel>
}
