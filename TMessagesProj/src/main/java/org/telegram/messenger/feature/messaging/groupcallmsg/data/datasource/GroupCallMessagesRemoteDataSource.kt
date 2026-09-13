package org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.voip.GroupCallMessagesController
import org.telegram.messenger.voip.VoIPService
import org.telegram.tgnet.TLRPC

class GroupCallMessagesRemoteDataSource(
    val account: Int
) : BaseRemoteDataSource(account) {

    suspend fun sendCallMessage(
        sendAsPeerId: Long,
        text: String,
        callId: Long
    ): Result<Boolean> {
        return runCatching {
            val service = VoIPService.getSharedInstance()
            val inputCall = service?.groupCall?.inputGroupCall

            val controller = runCatching { GroupCallMessagesController.getInstance(account) }.getOrNull()
            if (controller == null || inputCall == null) {
                return@runCatching Result.success(false)
            }

            val entities = TLRPC.TL_textWithEntities().apply {
                this.text = text
            }
            val success = controller.sendCallMessage(sendAsPeerId, entities, callId, inputCall)
            Result.success(success)
        }.getOrElse { e ->
            Result.failure(e.message ?: "Failed to send call message", e)
        }
    }
}
