package org.telegram.messenger.feature.media.stories.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.stories.data.mapper.StoryMapper
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.tl.TL_stories
import org.telegram.ui.Stories.StoriesController

class LegacyStoriesRepository(
    private val currentAccount: Int
) : StoriesRepository {

    private val storiesController: StoriesController
        get() = MessagesController.getInstance(currentAccount).storiesController

    override fun observeStories(): Flow<List<PeerStoriesModel>> {
        return NotificationCenterFlowBridge.observeEvents(
            currentAccount,
            NotificationCenter.storiesUpdated,
            NotificationCenter.storiesReadUpdated
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val list = storiesController.dialogListStories ?: emptyList<TL_stories.PeerStories>()
                    list.map { StoryMapper.mapPeerStories(it) }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeHiddenStories(): Flow<List<PeerStoriesModel>> {
        return NotificationCenterFlowBridge.observeEvents(
            currentAccount,
            NotificationCenter.storiesUpdated,
            NotificationCenter.storiesReadUpdated
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val list = storiesController.hiddenList ?: emptyList<TL_stories.PeerStories>()
                    list.map { StoryMapper.mapPeerStories(it) }
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeStealthMode(): Flow<StealthModeModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.stealthModeChanged
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val stealth = storiesController.stealthMode
                    val currentTime = ConnectionsManager.getInstance(currentAccount).currentTime
                    StoryMapper.mapStealthMode(stealth, currentTime)
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeSelfStories(): Flow<PeerStoriesModel?> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.storiesUpdated
        )
            .map { Unit }
            .onStart { emit(Unit) }
            .map {
                withContext(Dispatchers.Main) {
                    val myId = UserConfig.getInstance(currentAccount).clientUserId
                    val self = storiesController.getStories(myId)
                    if (self != null) StoryMapper.mapPeerStories(self) else null
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override suspend fun getStories(dialogId: Long): Result<List<StoryModel>> {
        return withContext(Dispatchers.Main) {
            try {
                val peerStories = storiesController.getStories(dialogId)
                if (peerStories != null) {
                    val maxReadId = peerStories.max_read_id
                    val mapped = (peerStories.stories ?: emptyList<TL_stories.StoryItem>()).map {
                        StoryMapper.mapStoryItem(it, dialogId, maxReadId)
                    }
                    Result.Success(mapped)
                } else {
                    Result.Success(emptyList())
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get stories for dialog $dialogId", e))
            }
        }
    }

    override suspend fun markStoryAsRead(dialogId: Long, storyId: Int): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val peerStories = storiesController.getStories(dialogId)
                val storyItem = peerStories?.stories?.firstOrNull { it.id == storyId }
                if (storyItem != null) {
                    storiesController.markStoryAsRead(peerStories, storyItem, false)
                    Result.Success(Unit)
                } else {
                    // Fallback using dummy StoryItem
                    val dummy = TL_stories.TL_storyItem()
                    dummy.id = storyId
                    dummy.dialogId = dialogId
                    storiesController.markStoryAsRead(dialogId, dummy)
                    Result.Success(Unit)
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to mark story $storyId as read", e))
            }
        }
    }

    override suspend fun deleteStory(dialogId: Long, storyId: Int): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val peerStories = storiesController.getStories(dialogId)
                val storyItem = peerStories?.stories?.firstOrNull { it.id == storyId }
                    ?: TL_stories.TL_storyItem().apply {
                        this.id = storyId
                        this.dialogId = dialogId
                    }
                storiesController.deleteStory(dialogId, storyItem)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to delete story $storyId", e))
            }
        }
    }

    override suspend fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val peerStories = storiesController.getStories(dialogId)
                val storyItem = peerStories?.stories?.firstOrNull { it.id == storyId }
                    ?: TL_stories.TL_storyItem().apply {
                        this.id = storyId
                        this.dialogId = dialogId
                    }
                val list = ArrayList<TL_stories.StoryItem>()
                list.add(storyItem)
                storiesController.updateStoriesPinned(dialogId, list, pin, null)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to update story pin status", e))
            }
        }
    }

    override suspend fun toggleStoryHidden(dialogId: Long, hide: Boolean): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                storiesController.toggleHidden(dialogId, hide, true, true)
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to toggle story hidden status", e))
            }
        }
    }

    override suspend fun activateStealthMode(future: Boolean, past: Boolean): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val req = TL_stories.TL_stories_activateStealthMode().apply {
                    this.future = future
                    this.past = past
                }
                val newStealth = TL_stories.TL_storiesStealthMode().apply {
                    flags = flags or (1 or 2)
                    cooldown_until_date = ConnectionsManager.getInstance(currentAccount).currentTime +
                        MessagesController.getInstance(currentAccount).stealthModeCooldown
                    active_until_date = ConnectionsManager.getInstance(currentAccount).currentTime +
                        MessagesController.getInstance(currentAccount).stealthModeFuture
                }
                storiesController.stealthMode = newStealth
                ConnectionsManager.getInstance(currentAccount).sendRequest(req) { _, _ -> }
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to activate stealth mode", e))
            }
        }
    }

    override suspend fun getStoryLimit(): Result<StoryLimitModel> {
        return withContext(Dispatchers.Main) {
            try {
                val isPremium = UserConfig.getInstance(currentAccount).isPremium
                val limit = if (isPremium) {
                    MessagesController.getInstance(currentAccount).storyExpiringLimitPremium
                } else {
                    MessagesController.getInstance(currentAccount).storyExpiringLimitDefault
                }
                val currentCount = storiesController.myStoriesCount
                Result.Success(StoryLimitModel(limit = limit, currentCount = currentCount))
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to get story limit", e))
            }
        }
    }

    override suspend fun refreshStories(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                storiesController.loadAllStories()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to refresh stories", e))
            }
        }
    }
}
