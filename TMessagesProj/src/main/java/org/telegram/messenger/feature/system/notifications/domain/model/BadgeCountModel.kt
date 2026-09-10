package org.telegram.messenger.feature.system.notifications.domain.model

data class BadgeCountModel(
    val totalUnreadCount: Int = 0,
    val badgeCount: Int = 0
)
