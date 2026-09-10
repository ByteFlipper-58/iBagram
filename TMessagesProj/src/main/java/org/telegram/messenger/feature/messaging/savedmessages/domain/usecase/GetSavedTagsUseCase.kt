package org.telegram.messenger.feature.messaging.savedmessages.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedTagModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository

/**
 * Encapsulates the business logic of retrieving and observing saved reaction tags.
 */
class GetSavedTagsUseCase(
    private val repository: SavedMessagesRepository
) {
    fun observe(): Flow<List<SavedTagModel>> =
        repository.observeSavedTags()
}
