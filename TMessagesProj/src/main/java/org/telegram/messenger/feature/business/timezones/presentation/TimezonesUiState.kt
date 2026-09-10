package org.telegram.messenger.feature.business.timezones.presentation

import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel

data class TimezonesUiState(
    val timezones: List<TimezoneModel> = emptyList(),
    val systemTimezoneId: String = "",
    val selectedTimezoneId: String? = null,
    val query: String = "",
    val filteredTimezones: List<TimezoneModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val displayedTimezones: List<TimezoneModel>
        get() = if (query.isBlank()) timezones else filteredTimezones

    val selectedTimezone: TimezoneModel?
        get() = timezones.firstOrNull { it.id == selectedTimezoneId }
}
