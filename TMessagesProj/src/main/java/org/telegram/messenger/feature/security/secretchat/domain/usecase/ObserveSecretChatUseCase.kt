package org.telegram.messenger.feature.security.secretchat.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.security.secretchat.domain.repository.SecretChatRepository

class ObserveSecretChatUseCase(
    private val repository: SecretChatRepository
) {
    operator fun invoke(chatId: Int): Flow<SecretChatModel?> {
        return repository.observeSecretChat(chatId)
    }
}
