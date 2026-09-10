package org.telegram.messenger.feature.media.gallerysave.domain.usecase

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

class RemoveGallerySaveExceptionUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType, dialogId: Long) {
        repository.removeException(peerType, dialogId)
    }
}
