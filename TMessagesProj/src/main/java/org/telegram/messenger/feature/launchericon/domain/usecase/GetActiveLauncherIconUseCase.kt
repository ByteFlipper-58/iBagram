package org.telegram.messenger.feature.launchericon.domain.usecase

import org.telegram.messenger.feature.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.launchericon.domain.repository.LauncherIconRepository

class GetActiveLauncherIconUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(): LauncherIconModel? = repository.getActiveIcon()
}
