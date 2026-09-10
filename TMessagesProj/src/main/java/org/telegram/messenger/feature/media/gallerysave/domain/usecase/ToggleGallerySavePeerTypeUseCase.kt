package org.telegram.messenger.feature.media.gallerysave.domain.usecase

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

class ToggleGallerySavePeerTypeUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType) {
        repository.togglePeerType(peerType)
    }
}
