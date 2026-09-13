package org.telegram.messenger.feature.messaging.groupcallmsg.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource.GroupCallMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource.GroupCallMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessageModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessageSendStatus
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository

class GroupCallMessagesRepositoryImpl(
    val account: Int,
    val localDataSource: GroupCallMessagesLocalDataSource,
    val remoteDataSource: GroupCallMessagesRemoteDataSource,
    val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : GroupCallMessagesRepository {

    override fun observeCallMessages(callId: Long): Flow<GroupCallMessagesStateModel> {
        return localDataSource.observeCallMessages(callId)
    }

    override fun getCallMessages(callId: Long): GroupCallMessagesStateModel {
        val list = localDataSource.getCallMessages(callId)
        return GroupCallMessagesStateModel(callId = callId, messages = list)
    }

    override fun sendCallMessage(callId: Long, sendAsPeerId: Long, text: String): Boolean {
        val randomId = System.currentTimeMillis() + (1..1000).random()
        val localMsg = GroupCallMessageModel(
            randomId = randomId,
            fromId = sendAsPeerId,
            text = text,
            isOut = true,
            sendStatus = GroupCallMessageSendStatus.CONFIRMED,
            timestamp = System.currentTimeMillis()
        )
        localDataSource.addCallMessage(callId, localMsg)

        val result = runBlocking(dispatcher) {
            remoteDataSource.sendCallMessage(sendAsPeerId, text, callId)
        }
        return when (result) {
            is Result.Success -> result.data
            is Result.Failure -> false
        }
    }

    override fun popMessage(callId: Long) {
        localDataSource.popCallMessage(callId)
    }

    override fun clearCallMessages(callId: Long) {
        localDataSource.clearCallMessages(callId)
    }
}
