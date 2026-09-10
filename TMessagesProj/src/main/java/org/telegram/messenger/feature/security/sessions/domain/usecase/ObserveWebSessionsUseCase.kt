package org.telegram.messenger.feature.security.sessions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class ObserveWebSessionsUseCase(
    private val repository: SessionsRepository
) {
    operator fun invoke(): Flow<List<WebSessionModel>> {
        return repository.observeWebSessions()
    }
}
