package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class EvaluatePipEligibilityUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(): Boolean = repository.canEnterPip()
}
