package org.telegram.messenger.feature.business.timezones.presentation

sealed interface TimezonesEvent {
    data class Load(val forceReload: Boolean = false) : TimezonesEvent
    data class Search(val query: String) : TimezonesEvent
    data class SelectTimezone(val timezoneId: String) : TimezonesEvent
    object ClearError : TimezonesEvent
}
