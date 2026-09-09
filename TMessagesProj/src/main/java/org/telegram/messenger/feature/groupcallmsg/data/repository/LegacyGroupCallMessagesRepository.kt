package org.telegram.messenger.feature.groupcallmsg.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.telegram.messenger.feature.groupcallmsg.data.mapper.GroupCallMessageMapper
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository
import org.telegram.messenger.voip.GroupCallMessage
import org.telegram.messenger.voip.GroupCallMessagesController
import org.telegram.messenger.voip.VoIPService
import org.telegram.tgnet.TLRPC
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository adapter bridging between Telegram's GroupCallMessagesController and domain layer.
 */
class LegacyGroupCallMessagesRepository(
    private val account: Int = 0
) : GroupCallMessagesRepository {

    private val inMemoryMessages = ConcurrentHashMap<Long, MutableList<GroupCallMessage>>()

    override fun observeCallMessages(callId: Long): Flow<GroupCallMessagesStateModel> = callbackFlow {
        val controller = getControllerSafely()
        if (controller == null) {
            trySend(getCallMessages(callId))
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : GroupCallMessagesController.CallMessageListener {
            override fun onNewGroupCallMessage(cId: Long, message: GroupCallMessage) {
                if (cId == callId || callId == 0L) {
                    trySend(getCallMessages(callId))
                }
            }

            override fun onPopGroupCallMessage() {
                trySend(getCallMessages(callId))
            }
        }

        try {
            controller.subscribeToCallMessages(callId, listener)
            trySend(getCallMessages(callId))
        } catch (_: Throwable) {
            trySend(getCallMessages(callId))
        }

        awaitClose {
            try {
                controller.unsubscribeFromCallMessages(callId, listener)
            } catch (_: Throwable) {
                // Ignore during teardown
            }
        }
    }

    override fun getCallMessages(callId: Long): GroupCallMessagesStateModel {
        val controller = getControllerSafely()
        val legacyList = if (controller != null) {
            try {
                controller.getCallMessages(callId)
            } catch (_: Throwable) {
                inMemoryMessages[callId] ?: emptyList()
            }
        } else {
            inMemoryMessages[callId] ?: emptyList()
        }

        val domainList = legacyList.map { GroupCallMessageMapper.toDomain(it) }
        return GroupCallMessagesStateModel(callId = callId, messages = domainList)
    }

    override fun sendCallMessage(callId: Long, sendAsPeerId: Long, text: String): Boolean {
        val controller = getControllerSafely()
        if (controller == null) {
            return false
        }

        return try {
            val entities = TLRPC.TL_textWithEntities()
            entities.text = text

            val service = VoIPService.getSharedInstance()
            val inputCall = service?.groupCall?.inputGroupCall

            if (inputCall != null) {
                controller.sendCallMessage(sendAsPeerId, entities, callId, inputCall)
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun popMessage(callId: Long) {
        val list = inMemoryMessages[callId]
        if (!list.isNullOrEmpty()) {
            list.removeAt(0)
        }
    }

    override fun clearCallMessages(callId: Long) {
        inMemoryMessages.remove(callId)
    }

    private fun getControllerSafely(): GroupCallMessagesController? {
        return try {
            GroupCallMessagesController.getInstance(account)
        } catch (_: Throwable) {
            null
        }
    }
}
