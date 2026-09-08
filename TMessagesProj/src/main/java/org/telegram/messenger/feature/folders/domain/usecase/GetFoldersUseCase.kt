package org.telegram.messenger.feature.folders.domain.usecase

import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class GetFoldersUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(): List<FolderModel> = repository.getFolders()
}
