package org.telegram.messenger.feature.messaging.drafts.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel

class DraftsLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val lock = Any()
    private val inMemoryDrafts = LinkedHashMap<Long, StoryDraftModel>()
    private val _stateFlow = MutableStateFlow(DraftsStateModel())
    val stateFlow: Flow<DraftsStateModel> = _stateFlow.asStateFlow()

    fun getState(): DraftsStateModel = synchronized(lock) { _stateFlow.value }

    fun setDrafts(drafts: List<StoryDraftModel>) = synchronized(lock) {
        inMemoryDrafts.clear()
        for (d in drafts) {
            inMemoryDrafts[d.id] = d
        }
        recomputeStateLocked()
    }

    fun saveDraft(draft: StoryDraftModel) = synchronized(lock) {
        inMemoryDrafts[draft.id] = draft
        recomputeStateLocked()
    }

    fun deleteDraft(draftId: Long) = synchronized(lock) {
        inMemoryDrafts.remove(draftId)
        recomputeStateLocked()
    }

    fun deleteDrafts(draftIds: List<Long>) = synchronized(lock) {
        for (id in draftIds) {
            inMemoryDrafts.remove(id)
        }
        recomputeStateLocked()
    }

    fun getDraftForEdit(peerId: Long, storyId: Int): StoryDraftModel? = synchronized(lock) {
        return inMemoryDrafts.values.firstOrNull { it.editStoryPeerId == peerId && it.editStoryId == storyId }
    }

    fun deleteForEdit(peerId: Long, storyId: Int) = synchronized(lock) {
        val toRemove = inMemoryDrafts.values.filter { it.editStoryPeerId == peerId && it.editStoryId == storyId }.map { it.id }
        for (id in toRemove) {
            inMemoryDrafts.remove(id)
        }
        recomputeStateLocked()
    }

    fun cleanupExpired(now: Long, expirationPeriodMs: Long): List<Long> = synchronized(lock) {
        val expiredIds = inMemoryDrafts.values
            .filter { (now - it.date) > expirationPeriodMs }
            .map { it.id }
        for (id in expiredIds) {
            inMemoryDrafts.remove(id)
        }
        if (expiredIds.isNotEmpty()) {
            recomputeStateLocked()
        }
        return expiredIds
    }

    fun setLoading(isLoading: Boolean) = synchronized(lock) {
        _stateFlow.value = _stateFlow.value.copy(isLoading = isLoading)
    }

    private fun recomputeStateLocked() {
        val list = inMemoryDrafts.values.toList()
        _stateFlow.value = _stateFlow.value.copy(
            drafts = list,
            isLoading = false
        )
    }
}
