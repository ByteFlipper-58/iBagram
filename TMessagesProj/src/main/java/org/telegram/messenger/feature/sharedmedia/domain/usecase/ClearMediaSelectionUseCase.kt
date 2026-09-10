package org.telegram.messenger.feature.sharedmedia.domain.usecase

import org.telegram.messenger.feature.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для сброса выделения элементов общего медиа.
 */
class ClearMediaSelectionUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke() {
        repository.clearSelection()
    }
}
