package org.telegram.messenger.feature.autodelete.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.autodelete.domain.model.GlobalAutoDeleteStateModel
import org.telegram.messenger.feature.autodelete.domain.repository.AutoDeleteRepository

class ObserveGlobalAutoDeleteUseCase(
    private val repository: AutoDeleteRepository
) {
    operator fun invoke(): Flow<GlobalAutoDeleteStateModel> {
        return repository.observeGlobalAutoDelete()
    }
}
