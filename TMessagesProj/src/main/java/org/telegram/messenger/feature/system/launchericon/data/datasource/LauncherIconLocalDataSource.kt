package org.telegram.messenger.feature.system.launchericon.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.feature.system.launchericon.data.mapper.LauncherIconMapper
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconsStateModel
import org.telegram.ui.LauncherIconController

/**
 * Local data source managing launcher icon component enablement and reactive state observation.
 */
open class LauncherIconLocalDataSource(
    private val currentAccount: Int = 0
) {
    private var isTestMode = false
    private var testActiveIcon: LauncherIconType = LauncherIconType.DEFAULT

    private val _stateFlow = MutableStateFlow(fetchState())

    fun setTestMode(activeIcon: LauncherIconType = LauncherIconType.DEFAULT) {
        isTestMode = true
        testActiveIcon = activeIcon
        _stateFlow.value = fetchState()
    }

    fun setTestActiveIcon(type: LauncherIconType) {
        testActiveIcon = type
        _stateFlow.value = fetchState()
    }

    private fun fetchState(): LauncherIconsStateModel {
        val icons = getLauncherIcons()
        val active = icons.firstOrNull { it.isEnabled } ?: icons.firstOrNull()
        return LauncherIconsStateModel(
            icons = icons,
            activeIcon = active
        )
    }

    open fun observeLauncherIcons(): Flow<LauncherIconsStateModel> = _stateFlow.asStateFlow()

    open fun getLauncherIcons(): List<LauncherIconModel> {
        val legacyIcons = LauncherIconController.LauncherIcon.values()
        return legacyIcons.map { legacyIcon ->
            val type = LauncherIconMapper.mapType(legacyIcon)
            val enabled = if (isTestMode) {
                type == testActiveIcon
            } else {
                try {
                    if (ApplicationLoader.applicationContext != null) {
                        LauncherIconController.isEnabled(legacyIcon)
                    } else {
                        type == LauncherIconType.DEFAULT
                    }
                } catch (_: Throwable) {
                    type == LauncherIconType.DEFAULT
                }
            }
            LauncherIconMapper.mapModel(legacyIcon, enabled)
        }
    }

    open fun getActiveIcon(): LauncherIconModel? {
        val icons = getLauncherIcons()
        return icons.firstOrNull { it.isEnabled } ?: icons.firstOrNull()
    }

    open fun isIconEnabled(type: LauncherIconType): Boolean {
        if (isTestMode) {
            return type == testActiveIcon
        }
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

    open fun setIcon(type: LauncherIconType): Boolean {
        if (isTestMode) {
            testActiveIcon = type
            _stateFlow.value = fetchState()
            return true
        }
        val legacyIcon = LauncherIconMapper.toLegacyIcon(type)
        return try {
            if (ApplicationLoader.applicationContext != null) {
                LauncherIconController.setIcon(legacyIcon)
            }
            _stateFlow.value = fetchState()
            true
        } catch (_: Throwable) {
            false
        }
    }

    open fun fixLauncherIconIfNeeded(): Boolean {
        if (isTestMode) {
            if (!getLauncherIcons().any { it.isEnabled }) {
                testActiveIcon = LauncherIconType.DEFAULT
            }
            _stateFlow.value = fetchState()
            return true
        }
        return try {
            if (ApplicationLoader.applicationContext != null) {
                LauncherIconController.tryFixLauncherIconIfNeeded()
            }
            _stateFlow.value = fetchState()
            true
        } catch (_: Throwable) {
            false
        }
    }
}
