package org.telegram.messenger.feature.folders.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.model.SuggestedFolderModel

/**
 * Clean domain repository contract for managing chat folders / filters.
 */
interface FoldersRepository {
    fun observeFolders(): Flow<List<FolderModel>>
    suspend fun getFolders(): List<FolderModel>
    suspend fun getFolder(id: Int): FolderModel?
    suspend fun createFolder(
        name: String,
        flags: Int = 0,
        includedPeerIds: List<Long> = emptyList(),
        excludedPeerIds: List<Long> = emptyList(),
        pinnedPeerIds: List<Long> = emptyList(),
        color: Int = -1
    ): Result<FolderModel>
    suspend fun updateFolder(folder: FolderModel): Result<Unit>
    suspend fun deleteFolder(id: Int): Result<Unit>
    suspend fun reorderFolders(folderIds: List<Int>): Result<Unit>
    suspend fun getSuggestedFolders(): List<SuggestedFolderModel>
}
