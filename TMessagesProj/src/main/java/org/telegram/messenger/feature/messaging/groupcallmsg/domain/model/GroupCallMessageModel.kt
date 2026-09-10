package org.telegram.messenger.feature.messaging.groupcallmsg.domain.model

/**
 * Pure domain model representing an ephemeral in-call text message in a group call or conference.
 */
data class GroupCallMessageModel(
    val randomId: Long,
    val fromId: Long,
    val text: String,
    val isOut: Boolean = false,
    val sendStatus: GroupCallMessageSendStatus = GroupCallMessageSendStatus.CONFIRMED,
    val reactionAnimatedEmojiId: Long = 0L,
    val emojicon: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPending: Boolean
        get() = sendStatus == GroupCallMessageSendStatus.SENDING || sendStatus == GroupCallMessageSendStatus.DELAYED

    val isSuccessful: Boolean
        get() = sendStatus == GroupCallMessageSendStatus.CONFIRMED

    val isFailed: Boolean
        get() = sendStatus == GroupCallMessageSendStatus.ERROR
}
