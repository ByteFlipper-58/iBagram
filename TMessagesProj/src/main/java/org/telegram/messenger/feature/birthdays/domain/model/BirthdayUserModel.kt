package org.telegram.messenger.feature.birthdays.domain.model

data class BirthdayUserModel(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String? = null,
    val photoPath: String? = null
) {
    val displayName: String
        get() = when {
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            !username.isNullOrBlank() -> username
            else -> id.toString()
        }
}
