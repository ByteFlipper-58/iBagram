package org.telegram.messenger.feature.security.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class AcceptQrLoginUseCase(
    private val repository: SessionsRepository
) {
    suspend fun byToken(token: ByteArray): Result<Unit> {
        return repository.acceptQrLogin(token)
    }

    suspend fun byLink(link: String): Result<Unit> {
        return repository.acceptQrLoginByLink(link)
    }
}
