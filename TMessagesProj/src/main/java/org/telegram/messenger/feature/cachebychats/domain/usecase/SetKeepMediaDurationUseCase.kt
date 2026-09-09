package org.telegram.messenger.feature.cachebychats.domain.usecase

import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository

class SetKeepMediaDurationUseCase(
    private val repository: CacheByChatsRepository
) {
    operator fun invoke(type: CacheChatType, duration: KeepMediaDuration) {
        repository.setDuration(type, duration)
    }
}
