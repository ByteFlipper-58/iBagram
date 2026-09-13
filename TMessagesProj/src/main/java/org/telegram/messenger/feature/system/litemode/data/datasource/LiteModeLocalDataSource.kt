package org.telegram.messenger.feature.system.litemode.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LiteMode
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.system.litemode.data.mapper.LiteModeMapper
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeState

/**
 * Local data source managing LiteMode flags, presets, power-saver threshold, and battery level observation.
 */
open class LiteModeLocalDataSource(
    private val currentAccount: Int = 0
) {
    private var isTestMode = false

    private val _state = MutableStateFlow(
        LiteModeState(
            rawFlags = LiteModePreset.HIGH.value,
            powerSaverThreshold = 10,
            batteryLevel = 100,
            isPowerSaverActive = false,
            isTablet = false,
            hasPremium = false
        )
    )

    init {
        loadFromLegacy()
    }

    fun setTestMode(initialState: LiteModeState = LiteModeState()) {
        isTestMode = true
        _state.value = initialState
    }

    private fun loadFromLegacy() {
        if (isTestMode) return
        try {
            val flags = LiteMode.getValue(true)
            val threshold = LiteMode.getPowerSaverLevel()
            val powerSaverActive = LiteMode.isPowerSaverApplied()
            val tablet = try { AndroidUtilities.isTablet() } catch (_: Throwable) { false }
            val premium = try { UserConfig.getInstance(currentAccount).isPremium } catch (_: Throwable) { false }

            _state.value = LiteModeState(
                rawFlags = flags,
                powerSaverThreshold = threshold,
                batteryLevel = 100,
                isPowerSaverActive = powerSaverActive,
                isTablet = tablet,
                hasPremium = premium
            )
        } catch (_: Throwable) {
            // Keep default in headless unit-tests
        }
    }

    open fun observeLiteModeState(): Flow<LiteModeState> = _state.asStateFlow()

    open fun getLiteModeState(): LiteModeState = _state.value

    open fun setFlagEnabled(flag: LiteModeFlag, enabled: Boolean) {
        val current = _state.value
        val legacyFlag = LiteModeMapper.mapFlagToLegacy(flag)
        val newRawFlags = if (enabled) {
            current.rawFlags or legacyFlag
        } else {
            current.rawFlags and legacyFlag.inv()
        }
        _state.value = current.copy(rawFlags = newRawFlags)

        if (!isTestMode) {
            try {
                LiteMode.setAllFlags(newRawFlags)
            } catch (_: Throwable) {}
        }
    }

    open fun setAllFlags(rawFlags: Int) {
        _state.value = _state.value.copy(rawFlags = rawFlags)
        if (!isTestMode) {
            try {
                LiteMode.setAllFlags(rawFlags)
            } catch (_: Throwable) {}
        }
    }

    open fun applyPreset(preset: LiteModePreset) {
        if (preset == LiteModePreset.CUSTOM) return
        val legacyValue = LiteModeMapper.mapPresetToLegacy(preset)
        _state.value = _state.value.copy(rawFlags = legacyValue)
        if (!isTestMode) {
            try {
                LiteMode.setAllFlags(legacyValue)
            } catch (_: Throwable) {}
        }
    }

    open fun setPowerSaverThreshold(percentage: Int) {
        val current = _state.value
        val isActive = current.batteryLevel <= percentage && percentage > 0
        _state.value = current.copy(
            powerSaverThreshold = percentage,
            isPowerSaverActive = isActive
        )
        if (!isTestMode) {
            try {
                LiteMode.setPowerSaverLevel(percentage)
            } catch (_: Throwable) {}
        }
    }

    open fun updateBatteryLevel(level: Int) {
        val current = _state.value
        val isActive = level <= current.powerSaverThreshold && current.powerSaverThreshold > 0
        _state.value = current.copy(
            batteryLevel = level,
            isPowerSaverActive = isActive
        )
    }

    open fun setHasPremium(hasPremium: Boolean) {
        _state.value = _state.value.copy(hasPremium = hasPremium)
    }

    open fun reload() {
        loadFromLegacy()
    }
}
