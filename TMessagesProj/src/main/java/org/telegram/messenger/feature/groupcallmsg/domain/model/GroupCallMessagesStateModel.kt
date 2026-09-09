package org.telegram.messenger.feature.groupcallmsg.domain.model

/**
 * Pure domain model encapsulating active in-call messages for a specific group call.
 */
data class GroupCallMessagesStateModel(
    val callId: Long = 0L,
    val messages: List<GroupCallMessageModel> = emptyList()
) {
    val activeMessagesCount: Int
        get() = messages.size

    val hasMessages: Boolean
        get() = messages.isNotEmpty()
}
