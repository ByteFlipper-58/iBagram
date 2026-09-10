package org.telegram.messenger.feature.ephemeralmessages.presentation

sealed class EphemeralMessagesEvent {
    data class InputTextChanged(val dialogId: Long, val text: String) : EphemeralMessagesEvent()
    data class RegisterAnchor(val dialogId: Long, val messageId: Int, val ephemeralMessageId: Int) : EphemeralMessagesEvent()
    data class UnregisterAnchor(val dialogId: Long, val messageId: Int, val ephemeralMessageId: Int) : EphemeralMessagesEvent()
    data class SelectDialog(val dialogId: Long) : EphemeralMessagesEvent()
    data class ClearDialogAnchors(val dialogId: Long) : EphemeralMessagesEvent()
    object ClearAllAnchors : EphemeralMessagesEvent()
    object DismissError : EphemeralMessagesEvent()
}
