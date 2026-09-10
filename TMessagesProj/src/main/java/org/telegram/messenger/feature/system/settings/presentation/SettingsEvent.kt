package org.telegram.messenger.feature.system.settings.presentation

/**
 * One-off UI events for settings.
 */
sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ShowError(val message: String?) : SettingsEvent()
    data class SettingChanged(val name: String) : SettingsEvent()
}
