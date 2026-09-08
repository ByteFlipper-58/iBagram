package org.telegram.messenger.feature.privacy.domain.usecase

import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class GetBlockedPeersUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(): List<Long> = repository.getBlockedPeers()
}
