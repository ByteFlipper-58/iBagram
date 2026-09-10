package org.telegram.messenger.feature.sharedmedia.domain.usecase

import org.telegram.messenger.feature.sharedmedia.domain.model.SharedMediaTabType
import org.telegram.messenger.feature.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для переключения активной вкладки общего медиа.
 */
class SelectSharedMediaTabUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke(tab: SharedMediaTabType) {
        repository.selectTab(tab)
    }
}
