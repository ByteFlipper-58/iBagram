package org.telegram.messenger.feature.system.litemode.presentation

import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModePreset

sealed class LiteModeEvent {
    data class SetFlag(val flag: LiteModeFlag, val enabled: Boolean) : LiteModeEvent()
    data class ToggleFlag(val flag: LiteModeFlag) : LiteModeEvent()
    data class ApplyPreset(val preset: LiteModePreset) : LiteModeEvent()
    data class SetPowerSaverThreshold(val percentage: Int) : LiteModeEvent()
    data class BatteryLevelChanged(val level: Int) : LiteModeEvent()
    data class PremiumChanged(val hasPremium: Boolean) : LiteModeEvent()
    object Reload : LiteModeEvent()
    object DismissError : LiteModeEvent()
}
