package org.telegram.messenger.feature.system.launchericon.domain.usecase

import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class GetLauncherIconsUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(): List<LauncherIconModel> = repository.getLauncherIcons()
}
