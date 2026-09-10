package org.telegram.messenger.feature.messaging.chatmeta.domain.usecase

import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository

class GetChatMetadataStatsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(): ChatMetadataStatsModel {
        return repository.getStats()
    }
}
