package org.telegram.messenger.feature.litemode.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.litemode.domain.model.LiteModeState

interface LiteModeRepository {
    fun observeLiteModeState(): Flow<LiteModeState>
    fun getLiteModeState(): LiteModeState
    suspend fun setFlagEnabled(flag: LiteModeFlag, enabled: Boolean)
    suspend fun setAllFlags(rawFlags: Int)
    suspend fun applyPreset(preset: LiteModePreset)
    suspend fun setPowerSaverThreshold(percentage: Int)
    suspend fun updateBatteryLevel(level: Int)
    suspend fun setHasPremium(hasPremium: Boolean)
    suspend fun reload()
}
