package org.telegram.messenger.feature.passkeys.domain.usecase

import org.telegram.messenger.feature.passkeys.domain.repository.PasskeysRepository

class CheckCanAddPasskeyUseCase(
    private val repository: PasskeysRepository
) {
    suspend operator fun invoke(): Boolean {
        if (!repository.isSupported()) return false
        val currentPasskeys = repository.getPasskeys().getOrNull() ?: emptyList()
        val maxPasskeys = repository.getMaxPasskeys()
        return currentPasskeys.size < maxPasskeys
    }
}
