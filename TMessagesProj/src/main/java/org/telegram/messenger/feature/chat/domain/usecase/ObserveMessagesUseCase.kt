package org.telegram.messenger.feature.chat.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.chat.domain.model.MessageModel
import org.telegram.messenger.feature.chat.domain.repository.ChatRepository

/**
 * Use case to observe the reactive stream of messages in a chat dialog.
 */
class ObserveMessagesUseCase(
    private val repository: ChatRepository
) {
    operator fun invoke(dialogId: Long): Flow<List<MessageModel>> {
        return repository.observeMessages(dialogId)
    }
}
