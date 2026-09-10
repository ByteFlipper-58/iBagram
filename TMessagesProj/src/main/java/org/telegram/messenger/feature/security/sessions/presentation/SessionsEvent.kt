package org.telegram.messenger.feature.security.sessions.presentation

sealed class SessionsEvent {
    data class LoadSessions(val silent: Boolean = false) : SessionsEvent()
    data class LoadWebSessions(val silent: Boolean = false) : SessionsEvent()
    data class TerminateSession(val hash: Long) : SessionsEvent()
    object TerminateAllOtherSessions : SessionsEvent()
    data class TerminateWebSession(val hash: Long) : SessionsEvent()
    object TerminateAllWebSessions : SessionsEvent()
    data class UpdateSessionSettings(
        val hash: Long,
        val acceptSecretChats: Boolean,
        val acceptCalls: Boolean
    ) : SessionsEvent()
    data class SetSessionsTtl(val ttlDays: Int) : SessionsEvent()
    data class AcceptQrLogin(val token: ByteArray) : SessionsEvent()
    data class AcceptQrLoginByLink(val link: String) : SessionsEvent()
    object ClearMessages : SessionsEvent()
}
