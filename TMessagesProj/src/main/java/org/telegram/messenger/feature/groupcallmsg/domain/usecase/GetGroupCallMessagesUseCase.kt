package org.telegram.messenger.feature.groupcallmsg.domain.usecase

import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository

class GetGroupCallMessagesUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long): GroupCallMessagesStateModel {
        return repository.getCallMessages(callId)
    }
}
