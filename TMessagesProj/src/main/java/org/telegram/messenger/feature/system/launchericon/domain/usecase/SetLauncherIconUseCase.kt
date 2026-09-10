package org.telegram.messenger.feature.system.launchericon.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class SetLauncherIconUseCase(
    private val repository: LauncherIconRepository
) {
    suspend operator fun invoke(type: LauncherIconType): Result<Boolean> = repository.setIcon(type)
}
