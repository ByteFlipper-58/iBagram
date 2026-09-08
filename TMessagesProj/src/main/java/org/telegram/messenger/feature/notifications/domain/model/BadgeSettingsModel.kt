package org.telegram.messenger.feature.notifications.domain.model

data class BadgeSettingsModel(
    val showBadgeNumber: Boolean = true,
    val showBadgeMuted: Boolean = false,
    val showBadgeMessages: Boolean = true
)
