package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaSelectionState

/**
 * Юзкейс для расчёта прав действий над выбранными элементами медиа (forward, delete, pin).
 */
class CalculateMediaSelectionUseCase {

    operator fun invoke(
        items: List<SharedMediaItem>,
        selectedIds: Set<Int>,
        canPinMessages: Boolean = true,
        canDeleteMessages: Boolean = true
    ): SharedMediaSelectionState {
        if (selectedIds.isEmpty()) {
            return SharedMediaSelectionState()
        }

        val canForward = true
        val canDelete = canDeleteMessages
        // Закрепить можно только одно сообщение за раз
        val canPin = canPinMessages && selectedIds.size == 1

        return SharedMediaSelectionState(
            selectedIds = selectedIds,
            canForward = canForward,
            canDelete = canDelete,
            canPin = canPin
        )
    }
}
