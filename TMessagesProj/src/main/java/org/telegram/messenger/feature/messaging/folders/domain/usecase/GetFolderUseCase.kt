package org.telegram.messenger.feature.messaging.folders.domain.usecase

import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository

class GetFolderUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(id: Int): FolderModel? = repository.getFolder(id)
}
