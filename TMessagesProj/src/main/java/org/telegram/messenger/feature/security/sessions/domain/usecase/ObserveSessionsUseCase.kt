package org.telegram.messenger.feature.security.sessions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class ObserveSessionsUseCase(
    private val repository: SessionsRepository
) {
    operator fun invoke(): Flow<SessionsListModel> {
        return repository.observeSessions()
    }
}
