package org.telegram.messenger.feature.folders.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class ObserveFoldersUseCase(
    private val repository: FoldersRepository
) {
    operator fun invoke(): Flow<List<FolderModel>> = repository.observeFolders()
}
