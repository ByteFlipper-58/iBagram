package org.telegram.messenger.feature.joinrequests.domain.model

data class JoinRequestUserModel(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String? = null,
    val phone: String? = null,
    val isBot: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
) {
    val displayName: String
        get() = when {
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            else -> username ?: id.toString()
        }
}
