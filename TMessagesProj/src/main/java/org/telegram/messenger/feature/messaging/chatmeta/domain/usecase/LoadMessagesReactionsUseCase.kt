package org.telegram.messenger.feature.messaging.chatmeta.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository

class LoadMessagesReactionsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        return repository.loadReactions(dialogId, messageIds)
    }
}
