package org.telegram.messenger.feature.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository

class TerminateSessionUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(hash: Long): Result<Unit> {
        return repository.terminateSession(hash)
    }
}
