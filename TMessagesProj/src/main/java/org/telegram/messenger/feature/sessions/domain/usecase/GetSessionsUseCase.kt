package org.telegram.messenger.feature.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository

class GetSessionsUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(): Result<SessionsListModel> {
        return repository.getSessions()
    }
}
