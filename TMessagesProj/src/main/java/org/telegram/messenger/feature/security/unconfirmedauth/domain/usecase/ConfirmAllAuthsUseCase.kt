package org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class ConfirmAllAuthsUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return repository.confirmAll()
    }
}
