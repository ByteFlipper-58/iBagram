package org.telegram.messenger.feature.system.appconfig.presentation

sealed interface AppConfigEvent {
    object Refresh : AppConfigEvent
    data class UpdateKey(val key: String, val value: Any) : AppConfigEvent
}
