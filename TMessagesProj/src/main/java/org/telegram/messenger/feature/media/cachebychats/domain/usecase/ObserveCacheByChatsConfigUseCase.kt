package org.telegram.messenger.feature.media.cachebychats.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

class ObserveCacheByChatsConfigUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(): Flow<CacheByChatsConfigModel> = repository.observeConfig()
}
