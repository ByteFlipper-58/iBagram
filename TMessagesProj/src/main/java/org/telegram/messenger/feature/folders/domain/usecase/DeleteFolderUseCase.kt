package org.telegram.messenger.feature.folders.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class DeleteFolderUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return repository.deleteFolder(id)
    }
}
