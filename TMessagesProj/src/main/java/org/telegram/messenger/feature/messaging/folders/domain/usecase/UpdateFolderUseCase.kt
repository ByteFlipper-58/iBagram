package org.telegram.messenger.feature.messaging.folders.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository

class UpdateFolderUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(folder: FolderModel): Result<Unit> {
        return repository.updateFolder(folder)
    }
}
