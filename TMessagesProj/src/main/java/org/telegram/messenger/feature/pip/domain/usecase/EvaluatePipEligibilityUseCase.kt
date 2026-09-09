package org.telegram.messenger.feature.pip.domain.usecase

import org.telegram.messenger.feature.pip.domain.repository.PipRepository

class EvaluatePipEligibilityUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(): Boolean = repository.canEnterPip()
}
