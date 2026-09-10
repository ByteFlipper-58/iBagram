package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaState
import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для наблюдения за состоянием общего медиа диалога.
 */
class ObserveSharedMediaStateUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke(): Flow<SharedMediaState> {
        return repository.observeState()
    }
}
