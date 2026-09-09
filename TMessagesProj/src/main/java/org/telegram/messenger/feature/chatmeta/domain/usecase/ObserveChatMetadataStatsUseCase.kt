package org.telegram.messenger.feature.chatmeta.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository

class ObserveChatMetadataStatsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(): Flow<ChatMetadataStatsModel> {
        return repository.observeStats()
    }
}
