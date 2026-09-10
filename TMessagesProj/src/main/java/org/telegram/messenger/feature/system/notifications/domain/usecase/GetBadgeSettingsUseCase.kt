package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class GetBadgeSettingsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(): BadgeSettingsModel = repository.getBadgeSettings()
}
