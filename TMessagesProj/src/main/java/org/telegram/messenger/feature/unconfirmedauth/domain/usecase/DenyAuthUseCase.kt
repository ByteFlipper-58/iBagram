package org.telegram.messenger.feature.unconfirmedauth.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class DenyAuthUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    suspend operator fun invoke(hash: Long): Result<Boolean> {
        return repository.denyAuth(hash)
    }
}
