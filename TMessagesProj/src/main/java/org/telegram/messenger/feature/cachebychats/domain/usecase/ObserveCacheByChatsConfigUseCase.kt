package org.telegram.messenger.feature.cachebychats.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository

class ObserveCacheByChatsConfigUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(): Flow<CacheByChatsConfigModel> = repository.observeConfig()
}
