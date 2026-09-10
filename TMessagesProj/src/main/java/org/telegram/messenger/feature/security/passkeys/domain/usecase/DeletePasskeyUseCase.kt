package org.telegram.messenger.feature.security.passkeys.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository

class DeletePasskeyUseCase(
    private val repository: PasskeysRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.deletePasskey(id)
    }
}
