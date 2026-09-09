package org.telegram.messenger.feature.pip.domain.usecase

import org.telegram.messenger.feature.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.pip.domain.repository.PipRepository

class RegisterPipSourceUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(source: PipSourceModel) {
        repository.registerSource(source)
    }
}
