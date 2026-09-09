package org.telegram.messenger.feature.launchericon.domain.usecase

import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.launchericon.domain.repository.LauncherIconRepository

class IsLauncherIconEnabledUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(type: LauncherIconType): Boolean = repository.isIconEnabled(type)
}
