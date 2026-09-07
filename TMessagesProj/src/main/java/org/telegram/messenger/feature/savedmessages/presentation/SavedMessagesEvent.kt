package org.telegram.messenger.feature.savedmessages.presentation

/**
 * One-time UI events dispatched by ViewModel to the view.
 */
sealed class SavedMessagesEvent {
    data class NavigateToChat(val dialogId: Long) : SavedMessagesEvent()
    data class ShowToast(val message: String) : SavedMessagesEvent()
    data class ShowError(val message: String) : SavedMessagesEvent()
}
