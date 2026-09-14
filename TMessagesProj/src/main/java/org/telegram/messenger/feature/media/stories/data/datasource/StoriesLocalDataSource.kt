package org.telegram.messenger.feature.media.stories.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.media.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.media.stories.domain.model.StoryModel
import java.util.concurrent.ConcurrentHashMap

class StoriesLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val storiesMap = ConcurrentHashMap<Long, MutableList<StoryModel>>()
    private val _dialogStoriesFlow = MutableStateFlow<List<PeerStoriesModel>>(emptyList())
    val dialogStoriesFlow: StateFlow<List<PeerStoriesModel>> = _dialogStoriesFlow.asStateFlow()

    private val _hiddenStoriesFlow = MutableStateFlow<List<PeerStoriesModel>>(emptyList())
    val hiddenStoriesFlow: StateFlow<List<PeerStoriesModel>> = _hiddenStoriesFlow.asStateFlow()

    private val _stealthModeFlow = MutableStateFlow(StealthModeModel(isActive = false))
    val stealthModeFlow: StateFlow<StealthModeModel> = _stealthModeFlow.asStateFlow()

    private val _selfStoriesFlow = MutableStateFlow<PeerStoriesModel?>(null)
    val selfStoriesFlow: StateFlow<PeerStoriesModel?> = _selfStoriesFlow.asStateFlow()

    fun setStories(dialogId: Long, stories: List<StoryModel>) {
        storiesMap[dialogId] = stories.toMutableList()
        updateFlows()
    }

    fun getStories(dialogId: Long): List<StoryModel> {
        return storiesMap[dialogId]?.toList() ?: emptyList()
    }

    fun markStoryAsRead(dialogId: Long, storyId: Int) {
        val list = storiesMap[dialogId] ?: return
        val index = list.indexOfFirst { it.id == storyId }
        if (index != -1) {
            list[index] = list[index].copy(isUnread = false)
            updateFlows()
        }
    }

    fun deleteStory(dialogId: Long, storyId: Int) {
        val list = storiesMap[dialogId] ?: return
        list.removeAll { it.id == storyId }
        updateFlows()
    }

    fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean) {
        val list = storiesMap[dialogId] ?: return
        val index = list.indexOfFirst { it.id == storyId }
        if (index != -1) {
            list[index] = list[index].copy(isPinned = pin)
            updateFlows()
        }
    }

    fun setStealthMode(active: Boolean, activeUntil: Long = 0L, cooldownUntil: Long = 0L) {
        _stealthModeFlow.value = StealthModeModel(
            isActive = active,
            activeUntilDate = activeUntil,
            cooldownUntilDate = cooldownUntil
        )
    }

    fun setSelfStories(stories: PeerStoriesModel?) {
        _selfStoriesFlow.value = stories
    }

    fun getStoryLimit(): StoryLimitModel {
        val current = storiesMap.values.sumOf { it.size }
        return StoryLimitModel(
            limit = 100,
            currentCount = current
        )
    }

    private fun updateFlows() {
        val peers = storiesMap.map { (dialogId, stories) ->
            PeerStoriesModel(
                dialogId = dialogId,
                stories = stories.toList(),
                maxReadId = stories.filter { !it.isUnread }.maxOfOrNull { it.id } ?: 0,
                hasUnread = stories.any { it.isUnread }
            )
        }
        _dialogStoriesFlow.value = peers
    }
}
