package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository

/**
 * Юзкейс для выделения/снятия выделения элемента общего медиа.
 */
class ToggleMediaSelectionUseCase(
    private val repository: SharedMediaRepository
) {
    operator fun invoke(messageId: Int) {
        repository.toggleItemSelection(messageId)
    }
}
