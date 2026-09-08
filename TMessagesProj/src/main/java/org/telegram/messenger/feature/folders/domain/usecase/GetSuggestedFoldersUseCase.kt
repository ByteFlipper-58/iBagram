package org.telegram.messenger.feature.folders.domain.usecase

import org.telegram.messenger.feature.folders.domain.model.SuggestedFolderModel
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository

class GetSuggestedFoldersUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(): List<SuggestedFolderModel> = repository.getSuggestedFolders()
}
