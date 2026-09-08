package org.telegram.messenger.feature.secretchat.domain.usecase

import org.telegram.messenger.feature.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository

class GetSecretChatUseCase(
    private val repository: SecretChatRepository
) {
    suspend operator fun invoke(chatId: Int): SecretChatModel? {
        return repository.getSecretChat(chatId)
    }
}
