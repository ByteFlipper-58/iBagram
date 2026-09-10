package org.telegram.messenger.feature.media.gallerysave.presentation

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel

/**
 * UI State for Auto-Save to Gallery settings and dialog exceptions.
 */
data class GallerySaveUiState(
    val config: GallerySaveConfigModel = GallerySaveConfigModel(),
    val selectedPeerType: GallerySavePeerType = GallerySavePeerType.PEER,
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val currentSettings: GallerySaveTargetSettingsModel
        get() = config.getSettings(selectedPeerType)

    val currentExceptionsCount: Int
        get() = config.getExceptions(selectedPeerType).size
}
