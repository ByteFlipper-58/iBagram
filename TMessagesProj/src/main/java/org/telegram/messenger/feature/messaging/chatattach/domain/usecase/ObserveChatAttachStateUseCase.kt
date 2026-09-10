package org.telegram.messenger.feature.messaging.chatattach.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.chatattach.domain.model.ChatAttachState
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository

class ObserveChatAttachStateUseCase(
    private val repository: ChatAttachRepository
) {
    operator fun invoke(): StateFlow<ChatAttachState> = repository.observeState()
}
