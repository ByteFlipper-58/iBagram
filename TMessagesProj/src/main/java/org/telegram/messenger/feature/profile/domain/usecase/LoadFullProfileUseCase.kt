package org.telegram.messenger.feature.profile.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.profile.domain.repository.ProfileRepository

/**
 * Use case to load full, detailed profile information from network or storage.
 */
class LoadFullProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): Result<ProfileModel> {
        return repository.loadFullProfile(id)
    }
}
