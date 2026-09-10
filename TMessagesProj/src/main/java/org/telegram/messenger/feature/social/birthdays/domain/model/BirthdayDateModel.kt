package org.telegram.messenger.feature.social.birthdays.domain.model

data class BirthdayDateModel(
    val day: Int,
    val month: Int,
    val year: Int? = null
)
