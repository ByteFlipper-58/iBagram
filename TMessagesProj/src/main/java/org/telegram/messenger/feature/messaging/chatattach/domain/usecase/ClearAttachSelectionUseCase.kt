package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class ClearAttachSelectionUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke() {
        repository.clearSelection()
    }
}
