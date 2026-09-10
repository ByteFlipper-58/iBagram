package org.telegram.messenger.feature.chatattach.domain.usecase

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachItem
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository

class ToggleAttachItemSelectionUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(item: ChatAttachItem): Boolean {
        val state = repository.getState()
        val exists = state.selectedItems.any { it.id == item.id }
        if (!exists && state.selectedItems.size >= state.maxSelectionLimit) {
            return false
        }
        repository.toggleItemSelection(item)
        return true
    }
}
