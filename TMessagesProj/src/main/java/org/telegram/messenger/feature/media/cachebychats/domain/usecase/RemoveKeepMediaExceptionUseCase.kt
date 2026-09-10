package org.telegram.messenger.feature.media.cachebychats.domain.usecase

import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

class RemoveKeepMediaExceptionUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(dialogId: Long, type: CacheChatType) {
        repository.removeException(dialogId, type)
    }
}
