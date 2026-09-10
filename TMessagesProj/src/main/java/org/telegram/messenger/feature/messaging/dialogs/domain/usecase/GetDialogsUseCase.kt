package org.telegram.messenger.feature.messaging.dialogs.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository

/**
 * Use case to observe the stream of chat dialogs in a folder.
 */
class GetDialogsUseCase(
    private val repository: DialogsRepository
) {
    operator fun invoke(folderId: Int = 0): Flow<List<DialogModel>> {
        return repository.getDialogs(folderId)
    }
}
