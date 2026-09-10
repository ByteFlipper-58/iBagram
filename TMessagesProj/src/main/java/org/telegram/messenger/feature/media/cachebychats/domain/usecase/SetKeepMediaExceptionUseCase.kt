package org.telegram.messenger.feature.media.cachebychats.domain.usecase

import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository

class SetKeepMediaExceptionUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(dialogId: Long, type: CacheChatType, duration: KeepMediaDuration) {
        repository.setException(dialogId, type, duration)
    }
}
