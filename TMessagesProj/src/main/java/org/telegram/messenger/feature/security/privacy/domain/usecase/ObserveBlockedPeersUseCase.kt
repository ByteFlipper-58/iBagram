package org.telegram.messenger.feature.security.privacy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class ObserveBlockedPeersUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(): Flow<List<Long>> = repository.observeBlockedPeers()
}
