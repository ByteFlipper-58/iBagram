package org.telegram.messenger.feature.messaging.stickers.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

class ToggleStickerSetInstalledUseCase(
    private val repository: StickersRepository
) {
    suspend operator fun invoke(id: Long, install: Boolean): Result<Unit> {
        return repository.toggleStickerSetInstalled(id, install)
    }
}
