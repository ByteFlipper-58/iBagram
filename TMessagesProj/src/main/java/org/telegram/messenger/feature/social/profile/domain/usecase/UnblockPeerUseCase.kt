package org.telegram.messenger.feature.social.profile.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository

/**
 * Use case to unblock a peer.
 */
class UnblockPeerUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return repository.unblockPeer(id)
    }
}
