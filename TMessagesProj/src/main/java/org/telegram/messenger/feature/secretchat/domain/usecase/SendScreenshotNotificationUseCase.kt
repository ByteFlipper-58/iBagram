package org.telegram.messenger.feature.secretchat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository

class SendScreenshotNotificationUseCase(
    private val repository: SecretChatRepository
) {
    suspend operator fun invoke(chatId: Int): Result<Unit> {
        return repository.sendScreenshotNotification(chatId)
    }
}
