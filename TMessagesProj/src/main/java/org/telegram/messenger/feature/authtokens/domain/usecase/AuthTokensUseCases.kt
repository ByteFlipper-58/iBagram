package org.telegram.messenger.feature.authtokens.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.authtokens.domain.model.AuthTokensState
import org.telegram.messenger.feature.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.messenger.feature.authtokens.domain.repository.AuthTokensRepository

class PruneTokensListUseCase {
    operator fun <T> invoke(tokens: List<T>, maxCount: Int = 20): List<T> {
        if (tokens.isEmpty()) return emptyList()
        return tokens.take(maxCount)
    }
}

class ValidateAuthTokenFormatUseCase {
    operator fun invoke(hexToken: String?): Boolean {
        if (hexToken.isNullOrBlank()) return false
        if (hexToken.length % 2 != 0) return false
        return hexToken.all { c ->
            (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
        }
    }
}

class ObserveAuthTokensStateUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke(): Flow<AuthTokensState> = repository.observeState()
}

class GetAuthTokensStateUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke(): AuthTokensState = repository.getState()
}

class GetSavedLoginTokensUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke(): List<SavedLoginTokenModel> = repository.getSavedLoginTokens()
}

class SaveLoginTokenUseCase(
    private val repository: AuthTokensRepository,
    private val validator: ValidateAuthTokenFormatUseCase = ValidateAuthTokenFormatUseCase()
) {
    operator fun invoke(token: SavedLoginTokenModel): Boolean {
        if (!validator(token.hexToken)) return false
        repository.saveLoginToken(token)
        return true
    }
}

class GetSavedLogoutTokensUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke(): List<SavedLogoutTokenModel> = repository.getSavedLogoutTokens()
}

class SaveLogoutTokensUseCase(
    private val repository: AuthTokensRepository,
    private val pruner: PruneTokensListUseCase = PruneTokensListUseCase()
) {
    operator fun invoke(tokens: List<SavedLogoutTokenModel>) {
        repository.saveLogoutTokens(pruner(tokens))
    }
}

class AddLogoutTokenUseCase(
    private val repository: AuthTokensRepository,
    private val validator: ValidateAuthTokenFormatUseCase = ValidateAuthTokenFormatUseCase()
) {
    operator fun invoke(token: SavedLogoutTokenModel): Boolean {
        if (!validator(token.hexToken)) return false
        repository.addLogoutToken(token)
        return true
    }
}

class RemoveTokenUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke(hexToken: String) {
        repository.removeLoginToken(hexToken)
        repository.removeLogoutToken(hexToken)
    }
}

class ClearAllTokensUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke() = repository.clearAllTokens()
}

class RefreshAuthTokensUseCase(
    private val repository: AuthTokensRepository
) {
    operator fun invoke() = repository.refresh()
}
