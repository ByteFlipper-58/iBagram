package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для применения фильтра Фото/Видео во вкладке Media.
 */
class SetSharedMediaFilterUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke(filter: SharedMediaFilterType) {
        repository.setFilter(filter)
    }
}
