package org.telegram.messenger.feature.birthdays.domain.model

data class ContactBirthdayModel(
    val contactId: Long,
    val birthday: BirthdayDateModel,
    val user: BirthdayUserModel? = null
)
