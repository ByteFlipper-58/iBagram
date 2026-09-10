package org.telegram.messenger.feature.system.launchericon.domain.usecase

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class IsLauncherIconEnabledUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(type: LauncherIconType): Boolean = repository.isIconEnabled(type)
}
