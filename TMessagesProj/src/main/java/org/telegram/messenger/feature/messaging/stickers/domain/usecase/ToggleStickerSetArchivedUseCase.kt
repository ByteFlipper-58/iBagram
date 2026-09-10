package org.telegram.messenger.feature.messaging.stickers.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

class ToggleStickerSetArchivedUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(id: Long, archive: Boolean): Result<Unit> {
        return repository.toggleStickerSetArchived(id, archive)
    }
}
