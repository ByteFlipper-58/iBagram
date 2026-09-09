package org.telegram.messenger.feature.chatmeta.domain.usecase

import org.telegram.messenger.feature.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository

class GetChatMetadataStatsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(): ChatMetadataStatsModel {
        return repository.getStats()
    }
}
