package org.telegram.messenger.feature.social.birthdays.presentation

import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel

data class BirthdaysUiState(
    val state: BirthdayStateModel? = null,
    val isBannerVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasTodayBirthdays: Boolean
        get() = state != null && !state.isTodayEmpty

    val todayBirthdaysCount: Int
        get() = state?.today?.size ?: 0
}
