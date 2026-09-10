package org.telegram.messenger.feature.media.autodeletemedia.presentation

sealed interface AutoDeleteMediaEvent {
    data class RunCleanup(val force: Boolean = false) : AutoDeleteMediaEvent
    data class LockFile(val path: String) : AutoDeleteMediaEvent
    data class UnlockFile(val path: String) : AutoDeleteMediaEvent
    object ClearLockedFiles : AutoDeleteMediaEvent
    object DismissError : AutoDeleteMediaEvent
}
