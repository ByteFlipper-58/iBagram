package org.telegram.messenger.feature.security.passkeys.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository

class GetPasskeysUseCase(
    private val repository: PasskeysRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<List<PasskeyModel>> {
        return repository.getPasskeys(force)
    }
}
