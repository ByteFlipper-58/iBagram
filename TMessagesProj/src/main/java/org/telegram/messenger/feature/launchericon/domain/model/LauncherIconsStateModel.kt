package org.telegram.messenger.feature.launchericon.domain.model

data class LauncherIconsStateModel(
    val icons: List<LauncherIconModel> = emptyList(),
    val activeIcon: LauncherIconModel? = null
)
