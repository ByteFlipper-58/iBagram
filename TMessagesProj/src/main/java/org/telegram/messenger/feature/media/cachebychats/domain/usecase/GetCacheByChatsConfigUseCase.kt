package org.telegram.messenger.feature.media.cachebychats.domain.usecase

import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

class GetCacheByChatsConfigUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(): CacheByChatsConfigModel = repository.getConfig()
}
