package org.telegram.messenger.feature.hints.domain.usecase

import org.telegram.messenger.feature.hints.domain.model.HintModel
import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository

class GetHintUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(type: HintType): HintModel {
        return repository.getHint(type)
    }
}
