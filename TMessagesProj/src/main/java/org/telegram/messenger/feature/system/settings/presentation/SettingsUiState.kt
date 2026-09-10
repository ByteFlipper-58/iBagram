package org.telegram.messenger.feature.system.settings.presentation

import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel

/**
 * UI State for settings screen.
 */
sealed class SettingsUiState {
    object Loading : SettingsUiState()

    data class Success(
        val settings: SettingsModel,
        val isUpdating: Boolean = false
    ) : SettingsUiState()

    data class Error(val message: String?) : SettingsUiState()
}
