package org.telegram.messenger.feature.security.unconfirmedauth.presentation

sealed interface UnconfirmedAuthEvent {
    object LoadAuths : UnconfirmedAuthEvent
    data class ConfirmAuth(val hash: Long) : UnconfirmedAuthEvent
    data class DenyAuth(val hash: Long) : UnconfirmedAuthEvent
    object ConfirmAll : UnconfirmedAuthEvent
    object DenyAll : UnconfirmedAuthEvent
    object ClearAll : UnconfirmedAuthEvent
    object ClearError : UnconfirmedAuthEvent
}
