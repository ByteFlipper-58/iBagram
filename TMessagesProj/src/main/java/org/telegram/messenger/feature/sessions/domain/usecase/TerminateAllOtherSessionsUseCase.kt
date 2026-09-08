package org.telegram.messenger.feature.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository

class TerminateAllOtherSessionsUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.terminateAllOtherSessions()
    }
}
