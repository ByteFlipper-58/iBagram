package org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.groupcallmsg.data.mapper.GroupCallMessageMapper
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessageModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.voip.GroupCallMessage
import org.telegram.messenger.voip.GroupCallMessagesController
import java.util.concurrent.ConcurrentHashMap

class GroupCallMessagesLocalDataSource(
    val account: Int
) {
    private val inMemoryMessages = ConcurrentHashMap<Long, MutableList<GroupCallMessageModel>>()
    private val stateFlows = ConcurrentHashMap<Long, MutableStateFlow<GroupCallMessagesStateModel>>()

    private fun getControllerSafely(): GroupCallMessagesController? {
        return runCatching { GroupCallMessagesController.getInstance(account) }.getOrNull()
    }

    fun getCallMessages(callId: Long): List<GroupCallMessageModel> {
        val controller = getControllerSafely()
        if (controller != null) {
            val legacy = runCatching { controller.getCallMessages(callId) }.getOrNull()
            if (legacy != null && legacy.isNotEmpty()) {
                return legacy.map { GroupCallMessageMapper.toDomain(it) }
            }
        }
        return inMemoryMessages[callId]?.toList() ?: emptyList()
    }

    fun addCallMessage(callId: Long, message: GroupCallMessageModel) {
        val list = inMemoryMessages.getOrPut(callId) { mutableListOf() }
        synchronized(list) {
            list.add(0, message)
        }
        notifyStateChanged(callId)
    }

    fun popCallMessage(callId: Long) {
        val list = inMemoryMessages[callId] ?: return
        synchronized(list) {
            if (list.isNotEmpty()) {
                list.removeAt(list.size - 1)
            }
        }
        notifyStateChanged(callId)
    }

    fun clearCallMessages(callId: Long) {
        inMemoryMessages.remove(callId)
        notifyStateChanged(callId)
    }

    fun observeCallMessages(callId: Long): Flow<GroupCallMessagesStateModel> = callbackFlow {
        val controller = getControllerSafely()
        if (controller == null) {
            val initial = GroupCallMessagesStateModel(callId = callId, messages = getCallMessages(callId))
            trySend(initial)
            val flow = stateFlows.getOrPut(callId) { MutableStateFlow(initial) }
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).run {
                // Return flow collection
            }
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : GroupCallMessagesController.CallMessageListener {
            override fun onNewGroupCallMessage(cId: Long, message: GroupCallMessage) {
                if (cId == callId || callId == 0L) {
                    trySend(GroupCallMessagesStateModel(callId = callId, messages = getCallMessages(callId)))
                }
            }

            override fun onPopGroupCallMessage() {
                trySend(GroupCallMessagesStateModel(callId = callId, messages = getCallMessages(callId)))
            }
        }

        try {
            controller.subscribeToCallMessages(callId, listener)
            trySend(GroupCallMessagesStateModel(callId = callId, messages = getCallMessages(callId)))
        } catch (_: Throwable) {
            trySend(GroupCallMessagesStateModel(callId = callId, messages = getCallMessages(callId)))
        }

        awaitClose {
            runCatching {
                controller.unsubscribeFromCallMessages(callId, listener)
            }
        }
    }

    private fun notifyStateChanged(callId: Long) {
        val currentMessages = inMemoryMessages[callId]?.toList() ?: emptyList()
        val newState = GroupCallMessagesStateModel(callId = callId, messages = currentMessages)
        stateFlows[callId]?.update { newState }
    }
}
