package org.telegram.messenger.feature.social.profile.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository

/**
 * Use case to block a peer.
 */
class BlockPeerUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return repository.blockPeer(id)
    }
}
