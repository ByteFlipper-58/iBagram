package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachLayoutType
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class SelectAttachLayoutUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(layout: ChatAttachLayoutType) {
        val state = repository.getState()
        if (state.availableLayouts.contains(layout)) {
            repository.selectLayout(layout)
        }
    }
}
