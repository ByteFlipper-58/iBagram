package org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class DenyAuthUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    suspend operator fun invoke(hash: Long): Result<Boolean> {
        return repository.denyAuth(hash)
    }
}
