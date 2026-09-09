package org.telegram.messenger.feature.gallerysave.domain.model

/**
 * Pure domain model encapsulating the complete configuration of Save-To-Gallery feature across all categories.
 */
data class GallerySaveConfigModel(
    val userSettings: GallerySaveTargetSettingsModel = GallerySaveTargetSettingsModel(GallerySavePeerType.PEER),
    val groupSettings: GallerySaveTargetSettingsModel = GallerySaveTargetSettingsModel(GallerySavePeerType.GROUP),
    val channelSettings: GallerySaveTargetSettingsModel = GallerySaveTargetSettingsModel(GallerySavePeerType.CHANNEL),
    val exceptions: Map<GallerySavePeerType, List<GallerySaveDialogExceptionModel>> = emptyMap()
) {
    fun getSettings(type: GallerySavePeerType): GallerySaveTargetSettingsModel {
        return when (type) {
            GallerySavePeerType.PEER -> userSettings
            GallerySavePeerType.GROUP -> groupSettings
            GallerySavePeerType.CHANNEL -> channelSettings
        }
    }

    fun getExceptions(type: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
        return exceptions[type] ?: emptyList()
    }
}
