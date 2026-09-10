package org.telegram.messenger.feature.litemode.presentation

import org.telegram.messenger.feature.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.litemode.domain.model.LiteModeState

data class LiteModeUiState(
    val state: LiteModeState = LiteModeState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentPreset: LiteModePreset get() = state.preset
    val isPowerSaverActive: Boolean get() = state.isPowerSaverActive
    val powerSaverThreshold: Int get() = state.powerSaverThreshold
    val batteryLevel: Int get() = state.batteryLevel

    fun isFlagActive(flag: LiteModeFlag): Boolean = state.isEnabled(flag)
    fun isFlagToggledInSettings(flag: LiteModeFlag): Boolean = state.isEnabledSetting(flag)
}
