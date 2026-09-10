package org.telegram.messenger.feature.security.privacy.domain.model

data class PrivacyRuleModel(
    val type: PrivacyRuleType,
    val mode: PrivacyRuleMode,
    val allowedUserIds: List<Long> = emptyList(),
    val disallowedUserIds: List<Long> = emptyList(),
    val allowedChatIds: List<Long> = emptyList(),
    val disallowedChatIds: List<Long> = emptyList()
)
