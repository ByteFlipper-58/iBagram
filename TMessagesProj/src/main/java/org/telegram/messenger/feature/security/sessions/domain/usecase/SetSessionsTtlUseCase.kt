package org.telegram.messenger.feature.security.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class SetSessionsTtlUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(ttlDays: Int): Result<Unit> {
        return repository.setSessionsTtl(ttlDays)
    }
}
