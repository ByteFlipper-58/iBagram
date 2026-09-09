package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class GetGallerySaveExceptionsUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
        return repository.getExceptions(peerType)
    }
}
