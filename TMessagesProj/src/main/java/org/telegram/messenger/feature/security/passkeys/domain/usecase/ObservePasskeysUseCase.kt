package org.telegram.messenger.feature.security.passkeys.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeysStateModel
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository

class ObservePasskeysUseCase(
    private val repository: PasskeysRepository
) {
    operator fun invoke(): Flow<PasskeysStateModel> {
        return repository.observePasskeys()
    }
}
