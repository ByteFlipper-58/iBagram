package org.telegram.messenger.feature.chat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chat.domain.model.MessageModel
import org.telegram.messenger.feature.chat.domain.repository.ChatRepository

/**
 * Use case to get the current list of messages in a chat dialog.
 */
class GetMessagesUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<List<MessageModel>> {
        return repository.getMessages(dialogId)
    }
}
