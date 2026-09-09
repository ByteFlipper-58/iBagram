package org.telegram.messenger.feature.groupcallmsg.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessageModel
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessagesStateModel

/**
 * Domain repository contract for in-call ephemeral messages during Telegram group calls and conferences.
 */
interface GroupCallMessagesRepository {
    fun observeCallMessages(callId: Long): Flow<GroupCallMessagesStateModel>
    fun getCallMessages(callId: Long): GroupCallMessagesStateModel
    fun sendCallMessage(callId: Long, sendAsPeerId: Long, text: String): Boolean
    fun popMessage(callId: Long)
    fun clearCallMessages(callId: Long)
}
