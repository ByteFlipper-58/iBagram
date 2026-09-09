package org.telegram.messenger.feature.chatmeta.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository

class CheckMessagesMetadataUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(
        dialogId: Long,
        items: List<MessageMetadataCheckItem>,
        currentTime: Long = System.currentTimeMillis()
    ): Result<ChatMetadataBatchResult> {
        return repository.checkMessages(dialogId, items, currentTime)
    }
}
