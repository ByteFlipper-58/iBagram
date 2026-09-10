package org.telegram.messenger.feature.messaging.folders.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.folders.data.mapper.FolderMapper
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.model.SuggestedFolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.messenger.support.LongSparseIntArray

/**
 * Clean data adapter implementing [FoldersRepository] backed by legacy [MessagesController]
 * and [MessagesStorage], executing on [Dispatchers.Main].
 */
class LegacyFoldersRepository(
    private val currentAccount: Int
) : FoldersRepository {

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val messagesStorage: MessagesStorage
        get() = MessagesStorage.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    private fun readFolders(): List<FolderModel> {
        val rawFilters = ArrayList(messagesController.dialogFilters)
        return FolderMapper.toDomainList(rawFilters)
    }

    override fun observeFolders(): Flow<List<FolderModel>> = callbackFlow {
        fun emitCurrent() {
            trySend(readFolders())
        }

        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            if (id == NotificationCenter.dialogFiltersUpdated || id == NotificationCenter.suggestedFiltersLoaded) {
                emitCurrent()
            }
        }

        notificationCenter.addObserver(delegate, NotificationCenter.dialogFiltersUpdated)
        notificationCenter.addObserver(delegate, NotificationCenter.suggestedFiltersLoaded)

        // Initial emission
        emitCurrent()

        awaitClose {
            notificationCenter.removeObserver(delegate, NotificationCenter.dialogFiltersUpdated)
            notificationCenter.removeObserver(delegate, NotificationCenter.suggestedFiltersLoaded)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getFolders(): List<FolderModel> = withContext(Dispatchers.Main) {
        readFolders()
    }

    override suspend fun getFolder(id: Int): FolderModel? = withContext(Dispatchers.Main) {
        val filter = messagesController.dialogFiltersById.get(id)
        if (filter != null) FolderMapper.toDomain(filter) else null
    }

    override suspend fun createFolder(
        name: String,
        flags: Int,
        includedPeerIds: List<Long>,
        excludedPeerIds: List<Long>,
        pinnedPeerIds: List<Long>,
        color: Int
    ): Result<FolderModel> = withContext(Dispatchers.Main) {
        try {
            var newId = 2
            while (messagesController.dialogFiltersById.get(newId) != null) {
                newId++
            }

            val filter = MessagesController.DialogFilter().apply {
                this.id = newId
                this.name = name
                this.flags = flags
                this.color = color
                this.order = messagesController.dialogFilters.size
                this.alwaysShow = ArrayList(includedPeerIds)
                this.neverShow = ArrayList(excludedPeerIds)
                val pinned = LongSparseIntArray()
                for (i in pinnedPeerIds.indices) {
                    pinned.put(pinnedPeerIds[i], i)
                }
                this.pinnedDialogs = pinned
                this.pendingUnreadCount = -1
                this.unreadCount = -1
            }

            messagesController.addFilter(filter, false)
            messagesStorage.saveDialogFilter(filter, false, true)
            notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)

            Result.success(FolderMapper.toDomain(filter))
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to create folder", e))
        }
    }

    override suspend fun updateFolder(folder: FolderModel): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val existing = messagesController.dialogFiltersById.get(folder.id)
            if (existing != null) {
                existing.name = folder.name
                existing.flags = folder.flags
                existing.color = folder.color
                existing.alwaysShow = ArrayList(folder.includedPeerIds)
                existing.neverShow = ArrayList(folder.excludedPeerIds)
                val pinned = LongSparseIntArray()
                for (i in folder.pinnedPeerIds.indices) {
                    pinned.put(folder.pinnedPeerIds[i], i)
                }
                existing.pinnedDialogs = pinned
                existing.pendingUnreadCount = -1
                existing.unreadCount = -1

                messagesController.onFilterUpdate(existing)
                messagesStorage.saveDialogFilter(existing, false, true)
                notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)

                Result.success(Unit)
            } else {
                Result.failure(AppError.NotFound("Folder with id ${folder.id} not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update folder", e))
        }
    }

    override suspend fun deleteFolder(id: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val existing = messagesController.dialogFiltersById.get(id)
            if (existing != null) {
                messagesController.removeFilter(existing)
                messagesStorage.deleteDialogFilter(existing)
                notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)

                Result.success(Unit)
            } else {
                Result.failure(AppError.NotFound("Folder with id $id not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to delete folder", e))
        }
    }

    override suspend fun reorderFolders(folderIds: List<Int>): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val newFilters = ArrayList<MessagesController.DialogFilter>()
            for (id in folderIds) {
                val f = messagesController.dialogFiltersById.get(id)
                if (f != null) {
                    f.order = newFilters.size
                    newFilters.add(f)
                }
            }
            messagesController.dialogFilters.clear()
            messagesController.dialogFilters.addAll(newFilters)
            messagesStorage.saveDialogFiltersOrder()
            notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to reorder folders", e))
        }
    }

    override suspend fun getSuggestedFolders(): List<SuggestedFolderModel> = withContext(Dispatchers.Main) {
        if (!messagesController.dialogFiltersLoaded) {
            messagesController.loadSuggestedFilters()
        }
        FolderMapper.toSuggestedList(messagesController.suggestedFilters)
    }
}
