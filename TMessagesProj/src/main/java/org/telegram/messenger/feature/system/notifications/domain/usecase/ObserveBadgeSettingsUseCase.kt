package org.telegram.messenger.feature.system.notifications.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class ObserveBadgeSettingsUseCase(
    private val repository: NotificationsRepository
) {
    operator fun invoke(): Flow<BadgeSettingsModel> = repository.observeBadgeSettings()
}
