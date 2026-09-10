package org.telegram.messenger.feature.messaging.folders.domain.usecase

import org.telegram.messenger.feature.messaging.folders.domain.model.SuggestedFolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository

class GetSuggestedFoldersUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(): List<SuggestedFolderModel> = repository.getSuggestedFolders()
}
