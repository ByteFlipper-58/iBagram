package org.telegram.messenger.feature.litemode.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LiteMode
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.litemode.data.mapper.LiteModeMapper
import org.telegram.messenger.feature.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.litemode.domain.model.LiteModeState
import org.telegram.messenger.feature.litemode.domain.repository.LiteModeRepository

class LegacyLiteModeRepository(
    private val currentAccount: Int = 0,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : LiteModeRepository {

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

    private fun loadFromLegacy() {
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

    override fun observeLiteModeState(): Flow<LiteModeState> = _state.asStateFlow()

    override fun getLiteModeState(): LiteModeState = _state.value

    override suspend fun setFlagEnabled(flag: LiteModeFlag, enabled: Boolean) = withContext(mainDispatcher) {
        val current = _state.value
        val legacyFlag = LiteModeMapper.mapFlagToLegacy(flag)
        val newRawFlags = if (enabled) {
            current.rawFlags or legacyFlag
        } else {
            current.rawFlags and legacyFlag.inv()
        }
        _state.value = current.copy(rawFlags = newRawFlags)

        try {
            LiteMode.setAllFlags(newRawFlags)
        } catch (_: Throwable) {}
    }

    override suspend fun setAllFlags(rawFlags: Int) = withContext(mainDispatcher) {
        _state.value = _state.value.copy(rawFlags = rawFlags)
        try {
            LiteMode.setAllFlags(rawFlags)
        } catch (_: Throwable) {}
    }

    override suspend fun applyPreset(preset: LiteModePreset) = withContext(mainDispatcher) {
        if (preset == LiteModePreset.CUSTOM) return@withContext
        val legacyValue = LiteModeMapper.mapPresetToLegacy(preset)
        _state.value = _state.value.copy(rawFlags = legacyValue)
        try {
            LiteMode.setAllFlags(legacyValue)
        } catch (_: Throwable) {}
    }

    override suspend fun setPowerSaverThreshold(percentage: Int) = withContext(mainDispatcher) {
        val current = _state.value
        val isActive = current.batteryLevel <= percentage && percentage > 0
        _state.value = current.copy(
            powerSaverThreshold = percentage,
            isPowerSaverActive = isActive
        )
        try {
            LiteMode.setPowerSaverLevel(percentage)
        } catch (_: Throwable) {}
    }

    override suspend fun updateBatteryLevel(level: Int) = withContext(mainDispatcher) {
        val current = _state.value
        val isActive = level <= current.powerSaverThreshold && current.powerSaverThreshold > 0
        _state.value = current.copy(
            batteryLevel = level,
            isPowerSaverActive = isActive
        )
    }

    override suspend fun setHasPremium(hasPremium: Boolean) = withContext(mainDispatcher) {
        _state.value = _state.value.copy(hasPremium = hasPremium)
    }

    override suspend fun reload() = withContext(mainDispatcher) {
        loadFromLegacy()
    }
}
