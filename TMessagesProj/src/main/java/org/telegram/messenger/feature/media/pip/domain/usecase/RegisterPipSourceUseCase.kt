package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class RegisterPipSourceUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(source: PipSourceModel) {
        repository.registerSource(source)
    }
}
