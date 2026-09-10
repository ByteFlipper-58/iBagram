package org.telegram.messenger.feature.messaging.groupcallmsg.presentation

import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessagesStateModel

/**
 * UI State for active group call messages.
 */
data class GroupCallMessagesUiState(
    val state: GroupCallMessagesStateModel = GroupCallMessagesStateModel(),
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    val messagesCount: Int
        get() = state.activeMessagesCount

    val hasMessages: Boolean
        get() = state.hasMessages
}
