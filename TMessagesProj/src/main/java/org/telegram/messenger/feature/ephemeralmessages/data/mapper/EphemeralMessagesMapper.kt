package org.telegram.messenger.feature.ephemeralmessages.data.mapper

import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralMessageIdHelper
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralMessageItem
import org.telegram.messenger.utils.EphemeralMessagesHelper
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_ephemeral

object EphemeralMessagesMapper {

    fun toDomain(ephemeralMessage: TL_ephemeral.EphemeralMessage): EphemeralMessageItem {
        val dialogId = when (val peer = ephemeralMessage.peer_id) {
            is TLRPC.TL_peerUser -> peer.user_id
            is TLRPC.TL_peerChat -> -peer.chat_id
            is TLRPC.TL_peerChannel -> -peer.channel_id
            else -> 0L
        }

        val fromId = when (val from = ephemeralMessage.from_id) {
            is TLRPC.TL_peerUser -> from.user_id
            is TLRPC.TL_peerChat -> -from.chat_id
            is TLRPC.TL_peerChannel -> -from.channel_id
            else -> 0L
        }

        return EphemeralMessageItem(
            id = ephemeralMessage.id,
            dialogId = dialogId,
            fromId = fromId,
            receiverBotId = ephemeralMessage.receiver_id,
            topMsgId = ephemeralMessage.top_msg_id,
            date = ephemeralMessage.date,
            message = ephemeralMessage.message ?: "",
            isWelcome = ephemeralMessage.welcome,
            anchorMsgId = ephemeralMessage.anchor_msg_id,
            viaBotId = ephemeralMessage.via_bot_id,
            isOut = ephemeralMessage.out,
            noForwards = ephemeralMessage.noforwards,
            invertMedia = ephemeralMessage.invert_media
        )
    }

    fun toDomain(message: TLRPC.Message): EphemeralMessageItem {
        val dialogId = when (val peer = message.peer_id) {
            is TLRPC.TL_peerUser -> peer.user_id
            is TLRPC.TL_peerChat -> -peer.chat_id
            is TLRPC.TL_peerChannel -> -peer.channel_id
            else -> 0L
        }

        val fromId = when (val from = message.from_id) {
            is TLRPC.TL_peerUser -> from.user_id
            is TLRPC.TL_peerChat -> -from.chat_id
            is TLRPC.TL_peerChannel -> -from.channel_id
            else -> 0L
        }

        val rawId = if (EphemeralMessageIdHelper.isEphemeral(message.id)) {
            EphemeralMessageIdHelper.unpack(message.id)
        } else {
            message.id
        }

        return EphemeralMessageItem(
            id = rawId,
            dialogId = dialogId,
            fromId = fromId,
            receiverBotId = message.ephemeralReceiverBotId,
            topMsgId = message.reply_to?.reply_to_top_id ?: 0,
            date = message.date,
            message = message.message ?: "",
            isWelcome = message.ephemeralReceiverBotId == -1L,
            anchorMsgId = message.ephemeralAnchorMsgId,
            viaBotId = message.via_bot_id,
            isOut = message.out,
            noForwards = message.noforwards,
            invertMedia = message.invert_media
        )
    }

    fun convertFakeDefaultToEphemeral(
        message: TLRPC.Message,
        topicId: Int
    ): TL_ephemeral.EphemeralMessage {
        return EphemeralMessagesHelper.convertFakeDefaultToEphemeral(message, topicId)
    }

    fun convertEphemeralToFakeDefault(
        ephemeralMessage: TL_ephemeral.EphemeralMessage
    ): TLRPC.TL_message {
        return EphemeralMessagesHelper.convertEphemeralToFakeDefault(ephemeralMessage)
    }
}
