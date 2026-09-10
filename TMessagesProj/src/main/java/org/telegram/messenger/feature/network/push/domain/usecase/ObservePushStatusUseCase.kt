package org.telegram.messenger.feature.network.push.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository

class ObservePushStatusUseCase(
    private val repository: PushRepository
) {
    operator fun invoke(): Flow<PushStatusModel> = repository.observePushStatus()
}
