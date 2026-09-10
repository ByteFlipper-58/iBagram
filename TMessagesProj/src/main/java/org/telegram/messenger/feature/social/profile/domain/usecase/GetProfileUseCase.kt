package org.telegram.messenger.feature.social.profile.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository

/**
 * Use case to get cached profile information for a peer.
 */
class GetProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): Result<ProfileModel> {
        return repository.getProfile(id)
    }
}
