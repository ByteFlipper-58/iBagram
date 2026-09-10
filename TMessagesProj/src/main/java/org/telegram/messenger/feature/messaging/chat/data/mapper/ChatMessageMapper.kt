package org.telegram.messenger.feature.messaging.chat.data.mapper

import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageDeliveryStatus
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel
import org.telegram.tgnet.TLRPC

/**
 * Pure mapper transforming legacy Telegram [MessageObject] into immutable [MessageModel] domain instances.
 */
object ChatMessageMapper {

    fun mapToDomain(messageObject: MessageObject): MessageModel {
        val text = messageObject.messageText?.toString()
            ?: messageObject.messageOwner?.message
            ?: ""

        val status = when {
            messageObject.isSendError() -> MessageDeliveryStatus.ERROR
            messageObject.isSending() -> MessageDeliveryStatus.SENDING
            else -> MessageDeliveryStatus.SENT
        }

        val replyId = messageObject.replyMessageObject?.id
            ?: (messageObject.messageOwner?.reply_to as? TLRPC.TL_messageReplyHeader)?.reply_to_msg_id

        return MessageModel(
            id = messageObject.id,
            dialogId = messageObject.dialogId,
            senderId = messageObject.fromChatId,
            text = text,
            date = messageObject.messageOwner?.date ?: 0,
            isOut = messageObject.isOut(),
            isUnread = messageObject.isUnread(),
            status = status,
            replyToMsgId = replyId
        )
    }

    fun mapToDomain(
        id: Int,
        dialogId: Long,
        senderId: Long,
        text: String,
        date: Int = 0,
        isOut: Boolean = false,
        isUnread: Boolean = false,
        status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
        replyToMsgId: Int? = null
    ): MessageModel {
        return MessageModel(
            id = id,
            dialogId = dialogId,
            senderId = senderId,
            text = text,
            date = date,
            isOut = isOut,
            isUnread = isUnread,
            status = status,
            replyToMsgId = replyToMsgId
        )
    }
}
