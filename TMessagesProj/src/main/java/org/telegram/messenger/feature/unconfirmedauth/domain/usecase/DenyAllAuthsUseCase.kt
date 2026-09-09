package org.telegram.messenger.feature.unconfirmedauth.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class DenyAllAuthsUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return repository.denyAll()
    }
}
