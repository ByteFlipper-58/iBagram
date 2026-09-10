package org.telegram.messenger.feature.social.profile.presentation

/**
 * One-shot UI events dispatched from ProfileViewModel.
 */
sealed class ProfileEvent {
    data class ShowToast(val message: String) : ProfileEvent()
    data class ShowError(val message: String?) : ProfileEvent()
    data class BlockStateChanged(val isBlocked: Boolean) : ProfileEvent()
}
