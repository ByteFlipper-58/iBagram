package org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase

import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository

class ClearGroupCallMessagesUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long) {
        repository.clearCallMessages(callId)
    }
}
