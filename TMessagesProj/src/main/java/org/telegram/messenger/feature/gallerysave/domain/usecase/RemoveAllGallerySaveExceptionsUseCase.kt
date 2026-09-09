package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class RemoveAllGallerySaveExceptionsUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType) {
        repository.removeAllExceptions(peerType)
    }
}
