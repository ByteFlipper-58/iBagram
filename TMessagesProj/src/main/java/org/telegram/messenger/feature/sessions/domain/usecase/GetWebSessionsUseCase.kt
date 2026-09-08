package org.telegram.messenger.feature.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.domain.model.WebSessionModel
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository

class GetWebSessionsUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(): Result<List<WebSessionModel>> {
        return repository.getWebSessions()
    }
}
