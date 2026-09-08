package org.telegram.messenger.feature.secretchat.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository

class ObserveSecretChatsUseCase(
    private val repository: SecretChatRepository
) {
    operator fun invoke(): Flow<List<SecretChatModel>> {
        return repository.observeSecretChats()
    }
}
