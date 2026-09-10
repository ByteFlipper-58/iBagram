package org.telegram.messenger.feature.security.sessions.presentation

import org.telegram.messenger.feature.security.sessions.domain.model.SessionModel
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel

data class SessionsUiState(
    val currentSession: SessionModel? = null,
    val otherSessions: List<SessionModel> = emptyList(),
    val passwordPendingSessions: List<SessionModel> = emptyList(),
    val webSessions: List<WebSessionModel> = emptyList(),
    val ttlDays: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
) {
    val totalActiveSessionsCount: Int
        get() = (if (currentSession != null) 1 else 0) + otherSessions.size + passwordPendingSessions.size

    val hasOtherSessions: Boolean
        get() = otherSessions.isNotEmpty() || passwordPendingSessions.isNotEmpty()
}
