package org.telegram.messenger.feature.messaging.chat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository

/**
 * Use case to delete messages in a chat dialog.
 */
class DeleteMessagesUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(dialogId: Long, messageIds: List<Int>, revoke: Boolean = true): Result<Unit> {
        return repository.deleteMessages(dialogId, messageIds, revoke)
    }
}
