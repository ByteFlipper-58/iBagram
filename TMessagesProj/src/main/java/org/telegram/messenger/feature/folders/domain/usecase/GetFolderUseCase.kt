package org.telegram.messenger.feature.folders.domain.usecase

import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class GetFolderUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(id: Int): FolderModel? = repository.getFolder(id)
}
