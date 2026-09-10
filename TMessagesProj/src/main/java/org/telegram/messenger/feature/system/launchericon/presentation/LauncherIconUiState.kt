package org.telegram.messenger.feature.system.launchericon.presentation

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel

data class LauncherIconUiState(
    val icons: List<LauncherIconModel> = emptyList(),
    val activeIcon: LauncherIconModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
