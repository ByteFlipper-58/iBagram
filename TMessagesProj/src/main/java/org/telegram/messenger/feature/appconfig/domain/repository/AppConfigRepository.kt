package org.telegram.messenger.feature.appconfig.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.appconfig.domain.model.AppGlobalConfigState

interface AppConfigRepository {
    fun observeConfig(): Flow<AppGlobalConfigState>
    fun getConfig(): AppGlobalConfigState
    suspend fun reloadConfig(): Result<AppGlobalConfigState>
    suspend fun updateConfigValue(key: String, value: Any): Result<Unit>
}
