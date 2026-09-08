package org.telegram.messenger.feature.dialogs.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.dialogs.domain.repository.DialogsRepository

/**
 * Use case to delete a dialog.
 */
class DeleteDialogUseCase(
    private val repository: DialogsRepository
) {
    suspend operator fun invoke(dialogId: Long, revoke: Boolean = true): Result<Unit> {
        return repository.deleteDialog(dialogId, revoke)
    }
}
