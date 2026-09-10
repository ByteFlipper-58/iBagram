package org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository

class ObserveGroupCallMessagesUseCase(
    private val repository: GroupCallMessagesRepository
) {
    operator fun invoke(callId: Long): Flow<GroupCallMessagesStateModel> {
        return repository.observeCallMessages(callId)
    }
}
