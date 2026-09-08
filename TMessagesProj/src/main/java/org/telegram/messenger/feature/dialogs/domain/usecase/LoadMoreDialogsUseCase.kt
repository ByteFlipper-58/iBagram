package org.telegram.messenger.feature.dialogs.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.dialogs.domain.repository.DialogsRepository

/**
 * Use case to load older dialogs for pagination.
 */
class LoadMoreDialogsUseCase(
    private val repository: DialogsRepository
) {
    suspend operator fun invoke(folderId: Int = 0, count: Int = 30): Result<Unit> {
        return repository.loadMoreDialogs(folderId, count)
    }
}
