package org.telegram.messenger.feature.messaging.folders.data.repository

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
import org.telegram.messenger.feature.messaging.folders.data.datasource.FoldersLocalDataSource
import org.telegram.messenger.feature.messaging.folders.data.datasource.FoldersRemoteDataSource
import org.telegram.messenger.feature.messaging.folders.data.mapper.FolderMapper
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.model.SuggestedFolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.messenger.support.LongSparseIntArray
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

/**
 * Clean repository implementation coordinating local and remote data sources
 * for Chat Folders (Dialog Filters), gradually displacing legacy monolithic logic.
 */
class FoldersRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: FoldersLocalDataSource,
    private val remoteDataSource: FoldersRemoteDataSource
) : FoldersRepository {

    private val controller: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val notificationCenter: NotificationCenter?
        get() = try {
            NotificationCenter.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    override fun observeFolders(): Flow<List<FolderModel>> = callbackFlow {
        fun emitCurrent() {
            val list = localDataSource.getRawDialogFilters()
            trySend(FolderMapper.toDomainList(list))
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            if (id == NotificationCenter.dialogFiltersUpdated || id == NotificationCenter.suggestedFiltersLoaded) {
                emitCurrent()
            }
        }

        NotificationCenterFlowBridge.runOnMainThread {
            try {
                notificationCenter?.addObserver(observer, NotificationCenter.dialogFiltersUpdated)
                notificationCenter?.addObserver(observer, NotificationCenter.suggestedFiltersLoaded)
            } catch (e: Throwable) {
                // Safely ignored in headless test environment
            }
            emitCurrent()
        }

        awaitClose {
            NotificationCenterFlowBridge.runOnMainThread {
                try {
                    notificationCenter?.removeObserver(observer, NotificationCenter.dialogFiltersUpdated)
                    notificationCenter?.removeObserver(observer, NotificationCenter.suggestedFiltersLoaded)
                } catch (e: Throwable) {
                    // Safely ignored in headless test environment
                }
            }
        }
    }

    override suspend fun getFolders(): List<FolderModel> = withContext(Dispatchers.Main) {
        val list = localDataSource.getRawDialogFilters()
        FolderMapper.toDomainList(list)
    }

    override suspend fun getFolder(id: Int): FolderModel? = withContext(Dispatchers.Main) {
        val filter = localDataSource.getRawDialogFilter(id)
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
            while (localDataSource.getRawDialogFilter(newId) != null) {
                newId++
            }

            val filter = MessagesController.DialogFilter().apply {
                this.id = newId
                this.name = name
                this.flags = flags
                this.color = color
                this.order = localDataSource.getRawDialogFilters().size
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

            // Sync with MTProto server
            val tlFilter = buildTlDialogFilter(newId, name, flags, color, includedPeerIds, excludedPeerIds, pinnedPeerIds)
            remoteDataSource.updateDialogFilter(newId, tlFilter)

            // Update in-memory state and persist to SQLite
            controller?.addFilter(filter, false)
            localDataSource.saveDialogFilter(filter, true)
            notifyFiltersUpdated()

            Result.success(FolderMapper.toDomain(filter))
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to create folder", e))
        }
    }

    override suspend fun updateFolder(folder: FolderModel): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val existing = localDataSource.getRawDialogFilter(folder.id)
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

                // Sync with MTProto server
                val tlFilter = buildTlDialogFilter(
                    folder.id,
                    folder.name,
                    folder.flags,
                    folder.color,
                    folder.includedPeerIds,
                    folder.excludedPeerIds,
                    folder.pinnedPeerIds
                )
                remoteDataSource.updateDialogFilter(folder.id, tlFilter)

                // Update in-memory state and persist to SQLite
                controller?.onFilterUpdate(existing)
                localDataSource.saveDialogFilter(existing, false)
                notifyFiltersUpdated()

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
            val existing = localDataSource.getRawDialogFilter(id)
            if (existing != null) {
                // Delete on MTProto server (passing null filter deletes it)
                remoteDataSource.updateDialogFilter(id, null)

                // Remove from in-memory cache and delete from SQLite
                controller?.removeFilter(existing)
                localDataSource.deleteDialogFilter(existing)
                notifyFiltersUpdated()

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
            val rawFilters = localDataSource.getRawDialogFilters()
            val newFilters = ArrayList<MessagesController.DialogFilter>()
            for (id in folderIds) {
                val f = localDataSource.getRawDialogFilter(id)
                if (f != null) {
                    f.order = newFilters.size
                    newFilters.add(f)
                }
            }

            // Sync order with MTProto server
            remoteDataSource.updateDialogFiltersOrder(folderIds)

            // Update in-memory order and persist to SQLite
            controller?.dialogFilters?.let { currentList ->
                currentList.clear()
                currentList.addAll(newFilters)
            }
            localDataSource.saveDialogFiltersOrder()
            notifyFiltersUpdated()

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to reorder folders", e))
        }
    }

    private fun notifyFiltersUpdated() {
        try {
            notificationCenter?.postNotificationName(NotificationCenter.dialogFiltersUpdated)
        } catch (e: Throwable) {
            // Safely ignored in headless test environment
        }
    }

    override suspend fun getSuggestedFolders(): List<SuggestedFolderModel> = withContext(Dispatchers.Main) {
        val rawSuggested = localDataSource.getRawSuggestedFilters()
        if (rawSuggested.isEmpty() && controller?.dialogFiltersLoaded == false) {
            controller?.loadSuggestedFilters()
        }
        FolderMapper.toSuggestedList(localDataSource.getRawSuggestedFilters())
    }

    private fun buildTlDialogFilter(
        id: Int,
        name: String,
        flags: Int,
        color: Int,
        includedPeerIds: List<Long>,
        excludedPeerIds: List<Long>,
        pinnedPeerIds: List<Long>
    ): TLRPC.TL_dialogFilter {
        val tl = TLRPC.TL_dialogFilter().apply {
            this.id = id
            this.title = TLRPC.TL_textWithEntities().apply { this.text = name }
            this.contacts = (flags and MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0
            this.non_contacts = (flags and MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0
            this.groups = (flags and MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0
            this.broadcasts = (flags and MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0
            this.bots = (flags and MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0
            this.exclude_muted = (flags and MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0
            this.exclude_read = (flags and MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0
            this.exclude_archived = (flags and MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0

            if (color < 0) {
                this.flags = this.flags and (134217728.inv())
                this.color = 0
            } else {
                this.flags = this.flags or 134217728
                this.color = color
            }
        }

        val mc = controller
        if (mc != null) {
            for (did in includedPeerIds) {
                val inputPeer = mc.getInputPeer(did)
                if (inputPeer != null) tl.include_peers.add(inputPeer)
            }
            for (did in excludedPeerIds) {
                val inputPeer = mc.getInputPeer(did)
                if (inputPeer != null) tl.exclude_peers.add(inputPeer)
            }
            for (did in pinnedPeerIds) {
                val inputPeer = mc.getInputPeer(did)
                if (inputPeer != null) tl.pinned_peers.add(inputPeer)
            }
        }
        return tl
    }
}
