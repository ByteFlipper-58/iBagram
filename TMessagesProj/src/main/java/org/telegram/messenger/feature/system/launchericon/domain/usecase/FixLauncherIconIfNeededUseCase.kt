package org.telegram.messenger.feature.system.launchericon.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

class FixLauncherIconIfNeededUseCase(
    private val repository: LauncherIconRepository
) {
    suspend operator fun invoke(): Result<Boolean> = repository.fixLauncherIconIfNeeded()
}
