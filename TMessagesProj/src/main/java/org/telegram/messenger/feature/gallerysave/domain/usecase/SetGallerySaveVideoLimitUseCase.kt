package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class SetGallerySaveVideoLimitUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType, limitBytes: Long) {
        repository.setVideoLimit(peerType, limitBytes)
    }
}
