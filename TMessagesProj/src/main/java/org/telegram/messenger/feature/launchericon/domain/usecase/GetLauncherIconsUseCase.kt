package org.telegram.messenger.feature.launchericon.domain.usecase

import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.launchericon.domain.repository.LauncherIconRepository

class GetLauncherIconsUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(): List<LauncherIconModel> = repository.getLauncherIcons()
}
