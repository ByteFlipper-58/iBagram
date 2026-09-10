package org.telegram.messenger.feature.system.notifications.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.model.BadgeSettingsModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository

class UpdateBadgeSettingsUseCase(
    private val repository: NotificationsRepository
) {
    suspend operator fun invoke(settings: BadgeSettingsModel): Result<Unit> =
        repository.updateBadgeSettings(settings)
}
