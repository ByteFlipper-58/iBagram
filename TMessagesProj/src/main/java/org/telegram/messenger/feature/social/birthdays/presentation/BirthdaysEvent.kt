package org.telegram.messenger.feature.social.birthdays.presentation

sealed class BirthdaysEvent {
    data class CheckBirthdays(val force: Boolean = false) : BirthdaysEvent()
    object DismissTodayBanner : BirthdaysEvent()
    object ClearError : BirthdaysEvent()
}
