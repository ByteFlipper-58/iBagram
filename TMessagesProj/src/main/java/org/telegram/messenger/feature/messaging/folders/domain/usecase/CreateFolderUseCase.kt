package org.telegram.messenger.feature.messaging.folders.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository

class CreateFolderUseCase(
    private val repository: FoldersRepository
) {
    suspend operator fun invoke(
        name: String,
        flags: Int = 0,
        includedPeerIds: List<Long> = emptyList(),
        excludedPeerIds: List<Long> = emptyList(),
        pinnedPeerIds: List<Long> = emptyList(),
        color: Int = -1
    ): Result<FolderModel> {
        return repository.createFolder(name, flags, includedPeerIds, excludedPeerIds, pinnedPeerIds, color)
    }
}
