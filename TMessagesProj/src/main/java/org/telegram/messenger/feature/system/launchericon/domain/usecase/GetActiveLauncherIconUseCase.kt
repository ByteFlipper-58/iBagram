package org.telegram.messenger.feature.system.launchericon.domain.usecase

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class GetActiveLauncherIconUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(): LauncherIconModel? = repository.getActiveIcon()
}
