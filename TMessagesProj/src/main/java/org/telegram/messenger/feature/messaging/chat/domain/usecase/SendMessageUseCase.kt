package org.telegram.messenger.feature.messaging.chat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository

/**
 * Use case to send a text message to a chat dialog.
 */
class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(dialogId: Long, text: String): Result<Unit> {
        return repository.sendMessage(dialogId, text)
    }
}
