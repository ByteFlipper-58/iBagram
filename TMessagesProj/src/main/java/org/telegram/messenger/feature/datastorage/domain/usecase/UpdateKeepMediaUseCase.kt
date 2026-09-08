package org.telegram.messenger.feature.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository

class UpdateKeepMediaUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(chatType: Int, keepMediaDuration: Int): Result<Unit> =
        repository.updateKeepMedia(chatType, keepMediaDuration)
}
