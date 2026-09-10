package org.telegram.messenger.feature.sharedmedia.domain.usecase

import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для получения текущего состояния общего медиа диалога.
 */
class GetSharedMediaStateUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke(): SharedMediaState {
        return repository.getState()
    }
}
