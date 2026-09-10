package org.telegram.messenger.feature.chatattach.domain.usecase

import org.telegram.messenger.feature.chatattach.domain.model.ChatAttachState
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository

class GetChatAttachStateUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(): ChatAttachState = repository.getState()
}
