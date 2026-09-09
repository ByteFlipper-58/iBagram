package org.telegram.messenger.feature.launchericon.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconsStateModel

interface LauncherIconRepository {
    fun observeLauncherIcons(): Flow<LauncherIconsStateModel>
    fun getLauncherIcons(): List<LauncherIconModel>
    fun getActiveIcon(): LauncherIconModel?
    fun isIconEnabled(type: LauncherIconType): Boolean
    suspend fun setIcon(type: LauncherIconType): Result<Boolean>
    suspend fun fixLauncherIconIfNeeded(): Result<Boolean>
}
