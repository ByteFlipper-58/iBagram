package org.telegram.messenger.feature.passkeys.domain.usecase

import org.telegram.messenger.feature.passkeys.domain.repository.PasskeysRepository

class IsPasskeysSupportedUseCase(
    private val repository: PasskeysRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.isSupported()
    }
}
