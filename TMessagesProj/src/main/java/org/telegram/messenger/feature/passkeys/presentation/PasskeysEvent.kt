package org.telegram.messenger.feature.passkeys.presentation

sealed interface PasskeysEvent {
    data class LoadPasskeys(val force: Boolean = false) : PasskeysEvent
    data class DeletePasskey(val id: String) : PasskeysEvent
    data object ClearError : PasskeysEvent
}
