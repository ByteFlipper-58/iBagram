package org.telegram.messenger.feature.cachebychats.domain.usecase

import org.telegram.messenger.feature.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository

class GetCacheByChatsConfigUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(): CacheByChatsConfigModel = repository.getConfig()
}
