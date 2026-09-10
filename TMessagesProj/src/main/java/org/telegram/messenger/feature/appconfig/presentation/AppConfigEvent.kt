package org.telegram.messenger.feature.appconfig.presentation

sealed interface AppConfigEvent {
    object Refresh : AppConfigEvent
    data class UpdateKey(val key: String, val value: Any) : AppConfigEvent
}
