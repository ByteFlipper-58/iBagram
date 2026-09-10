package org.telegram.messenger.feature.social.birthdays.domain.model

data class ContactBirthdayModel(
    val contactId: Long,
    val birthday: BirthdayDateModel,
    val user: BirthdayUserModel? = null
)
