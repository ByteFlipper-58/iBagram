package org.telegram.messenger.feature.social.profile.domain.model

/**
 * Unified pure domain model representing a Telegram peer profile
 * (user, bot, private chat, group, or broadcast channel).
 * Independent of Android SDK and legacy Telegram TLRPC data structures.
 */
data class ProfileModel(
    val id: Long,
    val title: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val isBot: Boolean = false,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val isBlocked: Boolean = false,
    val membersCount: Int? = null
)
