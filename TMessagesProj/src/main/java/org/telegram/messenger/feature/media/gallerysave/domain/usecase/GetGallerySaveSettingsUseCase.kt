package org.telegram.messenger.feature.media.gallerysave.domain.usecase

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

class GetGallerySaveSettingsUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel {
        return repository.getSettings(peerType)
    }
}
