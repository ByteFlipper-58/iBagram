package org.telegram.messenger.feature.security.passkeys.domain.model

data class PasskeyModel(
    val id: String,
    val name: String,
    val createdDate: Long,
    val lastUsageDate: Long? = null,
    val softwareEmojiId: Long = 0L,
)
