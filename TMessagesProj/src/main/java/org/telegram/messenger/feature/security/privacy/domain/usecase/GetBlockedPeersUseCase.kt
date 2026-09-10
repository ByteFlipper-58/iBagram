package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class GetBlockedPeersUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(): List<Long> = repository.getBlockedPeers()
}
