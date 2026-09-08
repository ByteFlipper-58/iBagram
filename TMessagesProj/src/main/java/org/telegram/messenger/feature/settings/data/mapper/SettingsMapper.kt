package org.telegram.messenger.feature.settings.data.mapper

import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.settings.domain.model.SettingsModel

/**
 * Pure mapper converting legacy [SharedConfig] and [UserConfig] properties into [SettingsModel].
 */
object SettingsMapper {

    fun map(userConfig: UserConfig?): SettingsModel {
        return SettingsModel(
            fontSize = SharedConfig.fontSize,
            bubbleRadius = SharedConfig.bubbleRadius,
            saveToGallery = SharedConfig.saveIncomingPhotos,
            streamMedia = SharedConfig.streamMedia,
            suggestStickers = SharedConfig.suggestStickers != 2,
            inappCamera = SharedConfig.inappCamera,
            distanceSystemType = SharedConfig.distanceSystemType,
            syncContacts = userConfig?.syncContacts ?: true,
            suggestContacts = userConfig?.suggestContacts ?: true,
            showCallsTab = userConfig?.showCallsTab ?: false
        )
    }

    fun mapFromValues(
        fontSize: Int = 16,
        bubbleRadius: Int = 17,
        saveToGallery: Boolean = false,
        streamMedia: Boolean = true,
        suggestStickers: Boolean = true,
        inappCamera: Boolean = true,
        distanceSystemType: Int = 0,
        syncContacts: Boolean = true,
        suggestContacts: Boolean = true,
        showCallsTab: Boolean = false
    ): SettingsModel {
        return SettingsModel(
            fontSize = fontSize,
            bubbleRadius = bubbleRadius,
            saveToGallery = saveToGallery,
            streamMedia = streamMedia,
            suggestStickers = suggestStickers,
            inappCamera = inappCamera,
            distanceSystemType = distanceSystemType,
            syncContacts = syncContacts,
            suggestContacts = suggestContacts,
            showCallsTab = showCallsTab
        )
    }
}
