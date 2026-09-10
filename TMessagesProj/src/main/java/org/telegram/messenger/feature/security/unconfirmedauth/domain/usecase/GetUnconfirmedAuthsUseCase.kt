package org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase

import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository

class GetUnconfirmedAuthsUseCase(
    private val repository: UnconfirmedAuthRepository
) {
    suspend operator fun invoke(): UnconfirmedAuthStateModel {
        return repository.getUnconfirmedAuths()
    }
}
