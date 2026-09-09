package org.telegram.messenger.feature.cachebychats.domain.usecase

import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository

class SetKeepMediaExceptionUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(dialogId: Long, type: CacheChatType, duration: KeepMediaDuration) {
        repository.setException(dialogId, type, duration)
    }
}
