package org.telegram.messenger.feature.push.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.push.domain.repository.PushRepository

class ObservePushStatusUseCase(
    private val repository: PushRepository
) {
    operator fun invoke(): Flow<PushStatusModel> = repository.observePushStatus()
}
