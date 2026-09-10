package org.telegram.messenger.feature.messaging.savedmessages.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Encapsulates the business logic of deleting a saved dialog.
 */
class DeleteSavedDialogUseCase(
    private val repository: SavedMessagesRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<Unit> =
        repository.deleteDialog(dialogId)
}
