package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class GetChatAttachStateUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(): ChatAttachState = repository.getState()
}
