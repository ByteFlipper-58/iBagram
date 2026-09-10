package org.telegram.messenger.feature.system.launchericon.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.data.mapper.LauncherIconMapper
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconsStateModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository
import org.telegram.ui.LauncherIconController

class LegacyLauncherIconRepository : LauncherIconRepository {

    private val stateFlow = MutableStateFlow(fetchState())

    private fun fetchState(): LauncherIconsStateModel {
        val icons = getLauncherIcons()
        val active = icons.firstOrNull { it.isEnabled } ?: icons.firstOrNull()
        return LauncherIconsStateModel(
            icons = icons,
            activeIcon = active
        )
    }

    override fun observeLauncherIcons(): Flow<LauncherIconsStateModel> = stateFlow.asStateFlow()

    override fun getLauncherIcons(): List<LauncherIconModel> {
        val legacyIcons = LauncherIconController.LauncherIcon.values()
        return legacyIcons.map { legacyIcon ->
            val enabled = try {
                if (ApplicationLoader.applicationContext != null) {
                    LauncherIconController.isEnabled(legacyIcon)
                } else {
                    legacyIcon == LauncherIconController.LauncherIcon.DEFAULT
                }
            } catch (_: Throwable) {
                legacyIcon == LauncherIconController.LauncherIcon.DEFAULT
            }
            LauncherIconMapper.mapModel(legacyIcon, enabled)
        }
    }

    override fun getActiveIcon(): LauncherIconModel? {
        val icons = getLauncherIcons()
        return icons.firstOrNull { it.isEnabled } ?: icons.firstOrNull()
    }

    override fun isIconEnabled(type: LauncherIconType): Boolean {
        val legacyIcon = LauncherIconMapper.toLegacyIcon(type)
        return try {
            if (ApplicationLoader.applicationContext != null) {
                LauncherIconController.isEnabled(legacyIcon)
            } else {
                type == LauncherIconType.DEFAULT
            }
        } catch (_: Throwable) {
            type == LauncherIconType.DEFAULT
        }
    }

    override suspend fun setIcon(type: LauncherIconType): Result<Boolean> = withContext(Dispatchers.Main) {
        val legacyIcon = LauncherIconMapper.toLegacyIcon(type)
        try {
            if (ApplicationLoader.applicationContext != null) {
                LauncherIconController.setIcon(legacyIcon)
            }
            stateFlow.value = fetchState()
            Result.Success(true)
        } catch (e: Exception) {
            Result.failure("Failed to set launcher icon: ${e.message}")
        }
    }

    override suspend fun fixLauncherIconIfNeeded(): Result<Boolean> = withContext(Dispatchers.Main) {
        try {
            if (ApplicationLoader.applicationContext != null) {
                LauncherIconController.tryFixLauncherIconIfNeeded()
            }
            stateFlow.value = fetchState()
            Result.Success(true)
        } catch (e: Exception) {
            Result.failure("Failed to fix launcher icon: ${e.message}")
        }
    }
}
