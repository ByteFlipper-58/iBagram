package org.telegram.messenger.feature.messaging.dialogs.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository

/**
 * Use case to mark all unread messages in a dialog as read.
 */
class MarkDialogAsReadUseCase(
    private val repository: DialogsRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<Unit> {
        return repository.markAsRead(dialogId)
    }
}
