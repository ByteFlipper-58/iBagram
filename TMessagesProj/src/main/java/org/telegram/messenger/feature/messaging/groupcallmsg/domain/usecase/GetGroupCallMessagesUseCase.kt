package org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase

import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository

class GetGroupCallMessagesUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long): GroupCallMessagesStateModel {
        return repository.getCallMessages(callId)
    }
}
