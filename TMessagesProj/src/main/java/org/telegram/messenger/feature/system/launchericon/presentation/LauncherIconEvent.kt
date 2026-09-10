package org.telegram.messenger.feature.system.launchericon.presentation

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType

sealed interface LauncherIconEvent {
    data class SelectIcon(val type: LauncherIconType) : LauncherIconEvent
    object FixIconIfNeeded : LauncherIconEvent
    object ClearError : LauncherIconEvent
}
