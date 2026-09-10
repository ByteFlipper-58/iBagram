package org.telegram.messenger.feature.messaging.dialogs.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository

/**
 * Use case to pin or unpin a dialog.
 */
class PinDialogUseCase(
    private val repository: DialogsRepository
) {
    suspend operator fun invoke(dialogId: Long, pin: Boolean): Result<Unit> {
        return repository.pinDialog(dialogId, pin)
    }
}
