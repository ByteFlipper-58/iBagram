package org.telegram.messenger.feature.messaging.autodelete.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.autodelete.domain.model.GlobalAutoDeleteStateModel
import org.telegram.messenger.feature.messaging.autodelete.domain.repository.AutoDeleteRepository

class ObserveGlobalAutoDeleteUseCase(
    private val repository: AutoDeleteRepository
) {
    operator fun invoke(): Flow<GlobalAutoDeleteStateModel> {
        return repository.observeGlobalAutoDelete()
    }
}
