package org.telegram.messenger.feature.system.launchericon.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconsStateModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class ObserveLauncherIconsUseCase(
    private val repository: LauncherIconRepository
) {
    operator fun invoke(): Flow<LauncherIconsStateModel> = repository.observeLauncherIcons()
}
