package org.telegram.messenger.feature.dialogs.presentation

/**
 * One-shot UI events dispatched to the View/Fragment/Activity.
 */
sealed class DialogsEvent {
    data class ShowToast(val message: String) : DialogsEvent()
    data class ShowError(val message: String?) : DialogsEvent()
    data class NavigateToChat(val dialogId: Long) : DialogsEvent()
}
