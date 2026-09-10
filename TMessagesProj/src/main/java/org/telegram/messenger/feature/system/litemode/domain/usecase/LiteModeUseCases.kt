package org.telegram.messenger.feature.system.litemode.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeState
import org.telegram.messenger.feature.system.litemode.domain.repository.LiteModeRepository

class CalculateEffectiveFlagsUseCase {
    operator fun invoke(
        rawFlags: Int,
        isPowerSaverActive: Boolean,
        hasPremium: Boolean,
        isTablet: Boolean
    ): Int {
        if (isPowerSaverActive) {
            return LiteModePreset.POWER_SAVER.value
        }
        var flags = rawFlags
        // Animated emoji keyboard
        if ((flags and (4 or 16384)) > 0) {
            flags = (flags and (4 or 16384).inv()) or if (hasPremium) 4 else 16384
        }
        // Animated emoji reactions
        if ((flags and (8 or 8192)) > 0) {
            flags = (flags and (8 or 8192).inv()) or if (hasPremium) 8 else 8192
        }
        // Animated emoji chat
        if ((flags and (16 or 4096)) > 0) {
            flags = (flags and (16 or 4096).inv()) or if (hasPremium) 16 else 4096
        }
        // Tablet two-column exception
        if (isTablet) {
            flags = flags or LiteModeFlag.CHAT_FORUM_TWOCOLUMN.bitMask
        }
        return flags
    }
}

class CheckLiteModeFlagUseCase(
    private val calculateEffectiveFlags: CalculateEffectiveFlagsUseCase = CalculateEffectiveFlagsUseCase()
) {
    operator fun invoke(state: LiteModeState, flag: LiteModeFlag): Boolean {
        if (flag == LiteModeFlag.CHAT_FORUM_TWOCOLUMN && state.isTablet) {
            return true
        }
        val effective = calculateEffectiveFlags(
            rawFlags = state.rawFlags,
            isPowerSaverActive = state.isPowerSaverActive,
            hasPremium = state.hasPremium,
            isTablet = state.isTablet
        )
        return (effective and flag.bitMask) != 0
    }
}

class ResolvePresetUseCase {
    operator fun invoke(rawFlags: Int): LiteModePreset = LiteModePreset.fromValue(rawFlags)
}

class ObserveLiteModeStateUseCase(private val repository: LiteModeRepository) {
    operator fun invoke(): Flow<LiteModeState> = repository.observeLiteModeState()
}

class GetLiteModeStateUseCase(private val repository: LiteModeRepository) {
    operator fun invoke(): LiteModeState = repository.getLiteModeState()
}

class ToggleLiteModeFlagUseCase(private val repository: LiteModeRepository) {
    suspend operator fun invoke(flag: LiteModeFlag, enabled: Boolean) {
        repository.setFlagEnabled(flag, enabled)
    }

    suspend fun toggle(flag: LiteModeFlag) {
        val current = repository.getLiteModeState()
        val isEnabled = current.isEnabledSetting(flag)
        repository.setFlagEnabled(flag, !isEnabled)
    }
}

class SetLiteModePresetUseCase(private val repository: LiteModeRepository) {
    suspend operator fun invoke(preset: LiteModePreset) {
        repository.applyPreset(preset)
    }
}

class UpdatePowerSaverThresholdUseCase(private val repository: LiteModeRepository) {
    suspend operator fun invoke(percentage: Int) {
        val clamped = percentage.coerceIn(0, 100)
        repository.setPowerSaverThreshold(clamped)
    }
}
