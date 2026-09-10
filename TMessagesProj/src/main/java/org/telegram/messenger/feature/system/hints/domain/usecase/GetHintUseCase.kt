package org.telegram.messenger.feature.system.hints.domain.usecase

import org.telegram.messenger.feature.system.hints.domain.model.HintModel
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository

class GetHintUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(type: HintType): HintModel {
        return repository.getHint(type)
    }
}
