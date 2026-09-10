package org.telegram.messenger.feature.system.launchericon.domain.model

data class LauncherIconModel(
    val type: LauncherIconType,
    val key: String,
    val titleRes: Int,
    val backgroundRes: Int,
    val foregroundRes: Int,
    val isPremium: Boolean,
    val isEnabled: Boolean
)
