package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveTargetSettingsModel
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class UpdateGallerySaveSettingsUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(settings: GallerySaveTargetSettingsModel) {
        repository.updateSettings(settings)
    }
}
