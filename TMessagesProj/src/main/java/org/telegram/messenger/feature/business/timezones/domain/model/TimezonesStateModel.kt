package org.telegram.messenger.feature.business.timezones.domain.model

data class TimezonesStateModel(
    val timezones: List<TimezoneModel> = emptyList(),
    val systemTimezoneId: String? = null,
    val isLoading: Boolean = false
) {
    val count: Int
        get() = timezones.size
}
