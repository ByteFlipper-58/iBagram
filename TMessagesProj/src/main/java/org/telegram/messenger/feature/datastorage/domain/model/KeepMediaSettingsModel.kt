package org.telegram.messenger.feature.datastorage.domain.model

data class KeepMediaSettingsModel(
    val keepMediaUser: Int = 2,      // 2 = KEEP_MEDIA_FOREVER
    val keepMediaGroup: Int = 1,     // 1 = KEEP_MEDIA_ONE_MONTH
    val keepMediaChannel: Int = 0,   // 0 = KEEP_MEDIA_ONE_WEEK
    val keepMediaStories: Int = 6    // 6 = KEEP_MEDIA_TWO_DAY
)
