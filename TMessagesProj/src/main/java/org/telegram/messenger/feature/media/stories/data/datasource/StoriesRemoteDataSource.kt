package org.telegram.messenger.feature.media.stories.data.datasource

import org.telegram.messenger.ApplicationLoader

class StoriesRemoteDataSource(
    private val currentAccount: Int = 0
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun markStoryAsRead(dialogId: Long, storyId: Int): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun deleteStory(dialogId: Long, storyId: Int): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun toggleStoryHidden(dialogId: Long, hide: Boolean): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun activateStealthMode(future: Boolean, past: Boolean): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun refreshStories(): Boolean {
        if (!isLegacyAvailable) return true
        return try {
            true
        } catch (_: Throwable) {
            false
        }
    }
}
