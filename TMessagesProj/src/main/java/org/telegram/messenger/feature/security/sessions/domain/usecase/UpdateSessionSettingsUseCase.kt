package org.telegram.messenger.feature.security.sessions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

class UpdateSessionSettingsUseCase(
    private val repository: SessionsRepository
) {
    suspend operator fun invoke(
        hash: Long,
        acceptSecretChats: Boolean,
        acceptCalls: Boolean
    ): Result<Unit> {
        return repository.updateSessionSettings(hash, acceptSecretChats, acceptCalls)
    }
}
