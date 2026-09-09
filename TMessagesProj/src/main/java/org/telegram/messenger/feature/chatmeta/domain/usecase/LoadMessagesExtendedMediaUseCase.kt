package org.telegram.messenger.feature.chatmeta.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository

class LoadMessagesExtendedMediaUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        return repository.loadExtendedMedia(dialogId, messageIds)
    }
}
