package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class BlockPrivacyPeerUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(peerId: Long): Result<Unit> = repository.blockPeer(peerId)
}
