package org.telegram.messenger.feature.groupcallmsg.domain.usecase

import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository

class ClearGroupCallMessagesUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long) {
        repository.clearCallMessages(callId)
    }
}
