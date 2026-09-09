package org.telegram.messenger.feature.push.domain.usecase

import org.telegram.messenger.feature.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.push.domain.repository.PushRepository

class GetPushStatusUseCase(
    private val repository: PushRepository
) {
    operator fun invoke(): PushStatusModel = repository.getPushStatus()
}
