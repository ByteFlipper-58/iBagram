package org.telegram.messenger.feature.system.launchericon.data.mapper

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.ui.LauncherIconController

object LauncherIconMapper {

    fun mapType(icon: LauncherIconController.LauncherIcon): LauncherIconType {
        return when (icon) {
            LauncherIconController.LauncherIcon.DEFAULT -> LauncherIconType.DEFAULT
            LauncherIconController.LauncherIcon.VINTAGE -> LauncherIconType.VINTAGE
            LauncherIconController.LauncherIcon.AQUA -> LauncherIconType.AQUA
            LauncherIconController.LauncherIcon.PREMIUM -> LauncherIconType.PREMIUM
            LauncherIconController.LauncherIcon.TURBO -> LauncherIconType.TURBO
            LauncherIconController.LauncherIcon.NOX -> LauncherIconType.NOX
        }
    }

    fun toLegacyIcon(type: LauncherIconType): LauncherIconController.LauncherIcon {
        return when (type) {
            LauncherIconType.DEFAULT -> LauncherIconController.LauncherIcon.DEFAULT
            LauncherIconType.VINTAGE -> LauncherIconController.LauncherIcon.VINTAGE
            LauncherIconType.AQUA -> LauncherIconController.LauncherIcon.AQUA
            LauncherIconType.PREMIUM -> LauncherIconController.LauncherIcon.PREMIUM
            LauncherIconType.TURBO -> LauncherIconController.LauncherIcon.TURBO
            LauncherIconType.NOX -> LauncherIconController.LauncherIcon.NOX
        }
    }

    fun mapModel(icon: LauncherIconController.LauncherIcon, isEnabled: Boolean): LauncherIconModel {
        return LauncherIconModel(
            type = mapType(icon),
            key = icon.key,
            titleRes = icon.title,
            backgroundRes = icon.background,
            foregroundRes = icon.foreground,
            isPremium = icon.premium,
            isEnabled = isEnabled
        )
    }
}
