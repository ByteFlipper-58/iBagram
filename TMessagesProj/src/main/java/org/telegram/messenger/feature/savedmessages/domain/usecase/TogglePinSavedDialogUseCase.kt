package org.telegram.messenger.feature.savedmessages.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Use case to toggle pinned state of a saved dialog.
 */
class TogglePinSavedDialogUseCase(
    private val repository: SavedMessagesRepository
) {
    suspend operator fun invoke(dialogId: Long, pinned: Boolean): Result<Unit> =
        repository.togglePin(dialogId, pinned)
}
