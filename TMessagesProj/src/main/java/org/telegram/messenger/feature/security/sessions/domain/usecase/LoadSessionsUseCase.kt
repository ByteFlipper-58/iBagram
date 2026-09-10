package org.telegram.messenger.feature.security.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class LoadSessionsUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(): Result<SessionsListModel> {
        return repository.loadSessions()
    }
}
