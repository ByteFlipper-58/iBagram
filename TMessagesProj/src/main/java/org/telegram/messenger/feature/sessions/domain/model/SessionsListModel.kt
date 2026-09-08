package org.telegram.messenger.feature.sessions.domain.model

data class SessionsListModel(
    val currentSession: SessionModel? = null,
    val otherSessions: List<SessionModel> = emptyList(),
    val passwordPendingSessions: List<SessionModel> = emptyList(),
    val ttlDays: Int = 0
)
