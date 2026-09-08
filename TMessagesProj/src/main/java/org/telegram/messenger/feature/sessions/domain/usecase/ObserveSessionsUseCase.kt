package org.telegram.messenger.feature.sessions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository

class ObserveSessionsUseCase(
    private val repository: SessionsRepository
) {
    operator fun invoke(): Flow<SessionsListModel> {
        return repository.observeSessions()
    }
}
