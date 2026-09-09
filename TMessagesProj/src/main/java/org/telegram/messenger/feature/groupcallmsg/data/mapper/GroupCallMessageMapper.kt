package org.telegram.messenger.feature.groupcallmsg.data.mapper

import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessageModel
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessageSendStatus
import org.telegram.messenger.voip.GroupCallMessage

/**
 * Pure mapper between legacy GroupCallMessage and pure domain model.
 */
object GroupCallMessageMapper {

    fun toDomain(legacy: GroupCallMessage): GroupCallMessageModel {
        val status = when {
            legacy.isSendConfirmed -> GroupCallMessageSendStatus.CONFIRMED
            legacy.isSendError -> GroupCallMessageSendStatus.ERROR
            legacy.isSendDelayed -> GroupCallMessageSendStatus.DELAYED
            legacy.isOut -> GroupCallMessageSendStatus.SENDING
            else -> GroupCallMessageSendStatus.CONFIRMED
        }

        val text = legacy.message?.text ?: ""
        val emojicon = legacy.visibleReaction?.emojicon

        return GroupCallMessageModel(
            randomId = legacy.randomId,
            fromId = legacy.fromId,
            text = text,
            isOut = legacy.isOut,
            sendStatus = status,
            reactionAnimatedEmojiId = legacy.reactionAnimatedEmojiId,
            emojicon = emojicon
        )
    }
}
