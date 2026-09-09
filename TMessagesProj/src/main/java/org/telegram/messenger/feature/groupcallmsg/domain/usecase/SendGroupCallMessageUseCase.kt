package org.telegram.messenger.feature.groupcallmsg.domain.usecase

import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository

class SendGroupCallMessageUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long, sendAsPeerId: Long, text: String): Boolean {
        return repository.sendCallMessage(callId, sendAsPeerId, text)
    }
}
