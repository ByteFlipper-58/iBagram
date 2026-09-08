package org.telegram.messenger.feature.secretchat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository

class StartSecretChatUseCase(
    private val repository: SecretChatRepository
) {
    suspend operator fun invoke(userId: Long): Result<Int> {
        return repository.startSecretChat(userId)
    }
}
