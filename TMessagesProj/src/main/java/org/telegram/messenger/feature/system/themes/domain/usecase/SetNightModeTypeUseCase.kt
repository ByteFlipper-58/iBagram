package org.telegram.messenger.feature.system.themes.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

class SetNightModeTypeUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(type: NightModeType): Result<Unit> {
        return repository.setNightModeType(type)
    }
}
