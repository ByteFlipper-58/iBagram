package org.telegram.messenger.feature.social.profile.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository

/**
 * Use case to observe the stream of profile changes for a peer.
 */
class ObserveProfileUseCase(
    private val repository: ProfileRepository
) {
    operator fun invoke(id: Long): Flow<ProfileModel?> {
        return repository.observeProfile(id)
    }
}
