package org.telegram.messenger.feature.notifications.domain.usecase

import org.telegram.messenger.feature.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository

class GetBadgeSettingsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): BadgeSettingsModel = repository.getBadgeSettings()
}
