package org.telegram.messenger.feature.chat.presentation

/**
 * One-shot UI events dispatched to the Chat view/activity.
 */
sealed class ChatEvent {
    data class ShowToast(val message: String) : ChatEvent()
    data class ShowError(val message: String?) : ChatEvent()
    data object MessageSent : ChatEvent()
    data object ScrollToBottom : ChatEvent()
}
