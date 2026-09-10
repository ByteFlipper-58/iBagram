package org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class ObserveUnconfirmedAuthsUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    operator fun invoke(): Flow<UnconfirmedAuthStateModel> {
        return repository.observeUnconfirmedAuths()
    }
}
