package org.telegram.messenger.feature.media.sharedmedia.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType

class SharedMediaRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun loadSharedMediaPage(
        dialogId: Long,
        tabType: SharedMediaTabType,
        filterType: SharedMediaFilterType,
        offsetId: Int,
        limit: Int = 50
    ): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }
}
