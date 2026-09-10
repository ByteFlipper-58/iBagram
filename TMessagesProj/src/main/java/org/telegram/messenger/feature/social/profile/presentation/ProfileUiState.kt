package org.telegram.messenger.feature.social.profile.presentation

import org.telegram.messenger.feature.social.profile.domain.model.ProfileModel

/**
 * UI state for the profile screen.
 */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()

    data class Success(
        val profile: ProfileModel,
        val isUpdatingBlockState: Boolean = false
    ) : ProfileUiState()

    data class Error(
        val message: String?
    ) : ProfileUiState()
}
