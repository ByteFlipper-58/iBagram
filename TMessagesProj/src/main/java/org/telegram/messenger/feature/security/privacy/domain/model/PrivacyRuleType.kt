package org.telegram.messenger.feature.security.privacy.domain.model

enum class PrivacyRuleType(val legacyType: Int) {
    LAST_SEEN(0),
    INVITE(1),
    CALLS(2),
    P2P(3),
    PHOTO(4),
    FORWARDS(5),
    PHONE(6),
    ADDED_BY_PHONE(7),
    VOICE_MESSAGES(8),
    BIO(9),
    BIRTHDAY(11),
    GIFTS(12),
    MESSAGES(13),
    MUSIC(14);

    companion object {
        fun fromLegacy(type: Int): PrivacyRuleType =
            entries.firstOrNull { it.legacyType == type } ?: LAST_SEEN
    }
}
