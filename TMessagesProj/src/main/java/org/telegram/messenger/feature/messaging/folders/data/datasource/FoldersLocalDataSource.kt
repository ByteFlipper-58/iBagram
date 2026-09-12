package org.telegram.messenger.feature.messaging.folders.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Local data source for Chat Folders (Dialog Filters) operations accessing MessagesStorage (SQLite)
 * and in-memory cache on the IO dispatcher.
 */
open class FoldersLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    private val controller: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    /**
     * Retrieves currently loaded dialog filters from the in-memory cache.
     */
    open fun getRawDialogFilters(): List<MessagesController.DialogFilter> {
        val list = controller?.dialogFilters ?: return emptyList()
        return ArrayList(list)
    }

    /**
     * Retrieves a dialog filter by its unique integer identifier.
     */
    open fun getRawDialogFilter(id: Int): MessagesController.DialogFilter? {
        return controller?.dialogFiltersById?.get(id)
    }

    /**
     * Retrieves suggested dialog filters from the in-memory cache.
     */
    open fun getRawSuggestedFilters(): List<TLRPC.TL_dialogFilterSuggested> {
        val list = controller?.suggestedFilters ?: return emptyList()
        return ArrayList(list)
    }

    /**
     * Persists a dialog filter into local MessagesStorage (SQLite database).
     */
    open suspend fun saveDialogFilter(
        filter: MessagesController.DialogFilter,
        isNew: Boolean
    ): Result<Unit> = runOnDb { storage ->
        storage.saveDialogFilter(filter, isNew, true)
    }

    /**
     * Deletes a dialog filter from local MessagesStorage (SQLite database).
     */
    open suspend fun deleteDialogFilter(
        filter: MessagesController.DialogFilter
    ): Result<Unit> = runOnDb { storage ->
        storage.deleteDialogFilter(filter)
    }

    /**
     * Persists dialog filter ordering into local MessagesStorage.
     */
    open suspend fun saveDialogFiltersOrder(): Result<Unit> = runOnDb { storage ->
        storage.saveDialogFiltersOrder()
    }
}
