package org.telegram.messenger.feature.cachebychats.domain.usecase

import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository

class ClearKeepMediaExceptionsUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(type: CacheChatType) {
        repository.clearAllExceptions(type)
    }
}
