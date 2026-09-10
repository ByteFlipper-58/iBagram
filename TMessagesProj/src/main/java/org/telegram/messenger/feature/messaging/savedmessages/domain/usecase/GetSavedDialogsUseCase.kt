package org.telegram.messenger.feature.messaging.savedmessages.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Encapsulates the business logic of retrieving and observing saved dialogs.
 */
class GetSavedDialogsUseCase(
    private val repository: SavedMessagesRepository
) {
    fun observe(): Flow<List<SavedDialogModel>> = repository.observeSavedDialogs()

    suspend operator fun invoke(): Result<List<SavedDialogModel>> =
        repository.getSavedDialogs()

    suspend fun refresh(): Result<Unit> =
        repository.loadDialogs(onlyCache = false)
}
