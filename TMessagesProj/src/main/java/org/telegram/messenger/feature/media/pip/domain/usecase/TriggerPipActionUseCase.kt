package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class TriggerPipActionUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(tag: String, actionId: Int) {
        repository.triggerPipAction(tag, actionId)
    }
}
