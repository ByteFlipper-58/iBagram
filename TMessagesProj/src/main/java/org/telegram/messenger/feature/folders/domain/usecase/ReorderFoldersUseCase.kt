package org.telegram.messenger.feature.folders.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class ReorderFoldersUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(folderIds: List<Int>): Result<Unit> {
        return repository.reorderFolders(folderIds)
    }
}
