package org.telegram.messenger.feature.media.gallerysave.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel

/**
 * Remote data source managing cloud/server synchronization for Save-To-Gallery preferences.
 */
open class GallerySaveRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchRemoteDefaults(): GallerySaveConfigModel {
        return GallerySaveConfigModel(
            userSettings = GallerySaveTargetSettingsModel(GallerySavePeerType.PEER),
            groupSettings = GallerySaveTargetSettingsModel(GallerySavePeerType.GROUP),
            channelSettings = GallerySaveTargetSettingsModel(GallerySavePeerType.CHANNEL)
        )
    }

    open suspend fun syncConfig(config: GallerySaveConfigModel): Boolean {
        // Sync configuration state with remote account settings if applicable
        return true
    }
}
