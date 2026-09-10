package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.model.PipState
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class DispatchPipStateUseCase(
    private val repository: PipRepository
) {
    operator fun invoke(state: PipState, byActivityStop: Boolean = false) {
        repository.updatePipState(state, byActivityStop)
    }
}
