package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class UnregisterPipSourceUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(tag: String) {
        repository.unregisterSource(tag)
    }
}
