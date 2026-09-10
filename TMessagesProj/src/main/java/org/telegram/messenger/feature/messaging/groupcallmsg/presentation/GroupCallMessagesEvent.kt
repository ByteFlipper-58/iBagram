package org.telegram.messenger.feature.messaging.groupcallmsg.presentation

/**
 * MVI Events for GroupCallMessagesViewModel.
 */
sealed interface GroupCallMessagesEvent {
    data class SetCallId(val callId: Long) : GroupCallMessagesEvent
    data class SendMessage(val text: String, val sendAsPeerId: Long) : GroupCallMessagesEvent
    object PopMessage : GroupCallMessagesEvent
    object ClearMessages : GroupCallMessagesEvent
    object DismissError : GroupCallMessagesEvent
}
