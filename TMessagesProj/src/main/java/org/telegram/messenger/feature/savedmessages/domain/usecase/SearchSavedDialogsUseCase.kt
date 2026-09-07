package org.telegram.messenger.feature.savedmessages.domain.usecase

import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Encapsulates searching saved dialogs by query text.
 */
class SearchSavedDialogsUseCase(
    private val repository: SavedMessagesRepository
) {
    operator fun invoke(query: String): List<SavedDialogModel> =
        repository.searchDialogs(query)
}
