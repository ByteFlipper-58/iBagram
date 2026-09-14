package org.telegram.messenger.feature.media.voip.data.datasource

import org.telegram.messenger.ApplicationLoader

class VoIPRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun initiateCall(userId: Long, isVideo: Boolean): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun acceptCall(): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun declineCall(): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hangUp(): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }
}
