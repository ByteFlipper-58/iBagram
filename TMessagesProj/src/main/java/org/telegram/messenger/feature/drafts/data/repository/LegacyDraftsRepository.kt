package org.telegram.messenger.feature.drafts.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.drafts.data.mapper.DraftsMapper
import org.telegram.messenger.feature.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository
import org.telegram.ui.Stories.recorder.DraftsController
import org.telegram.ui.Stories.recorder.StoryEntry

/**
 * Legacy implementation of [DraftsRepository] adapting [DraftsController] and [MessagesStorage].
 */
class LegacyDraftsRepository(
    private val account: Int
) : DraftsRepository {

    private val lock = Any()
    private val inMemoryDrafts = LinkedHashMap<Long, StoryDraftModel>()

    private val _stateFlow = MutableStateFlow(DraftsStateModel())
    override fun observeDraftsState(): Flow<DraftsStateModel> = _stateFlow.asStateFlow()
    override fun getDraftsState(): DraftsStateModel = _stateFlow.value

    init {
        try {
            NotificationCenter.getInstance(account).addObserver({ _, id, _ ->
                if (id == NotificationCenter.storiesDraftsUpdated) {
                    syncFromLegacy()
                }
            }, NotificationCenter.storiesDraftsUpdated)
        } catch (_: Throwable) {
            // Headless unit test mode
        }
    }

    private fun getLegacyController(): DraftsController? {
        return try {
            MessagesController.getInstance(account)?.storiesController?.draftsController
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun loadDrafts(force: Boolean): Unit = withContext(Dispatchers.Main) {
        val controller = getLegacyController()
        if (controller != null) {
            try {
                controller.load()
                syncFromLegacy()
            } catch (_: Throwable) {
                // Fallback
            }
        } else {
            synchronized(lock) {
                _stateFlow.value = _stateFlow.value.copy(
                    drafts = inMemoryDrafts.values.toList(),
                    isLoading = false
                )
            }
        }
    }

    override suspend fun saveDraft(draft: StoryDraftModel): Unit = withContext(Dispatchers.Main) {
        synchronized(lock) {
            inMemoryDrafts[draft.id] = draft
            recomputeStateLocked()
        }
    }

    override suspend fun deleteDraft(draftId: Long): Unit = withContext(Dispatchers.Main) {
        val controller = getLegacyController()
        if (controller != null) {
            try {
                val legacyEntry = controller.drafts.firstOrNull { it.draftId == draftId }
                if (legacyEntry != null) {
                    controller.delete(legacyEntry)
                }
            } catch (_: Throwable) {
                // ignore
            }
        }
        synchronized(lock) {
            inMemoryDrafts.remove(draftId)
            recomputeStateLocked()
        }
    }

    override suspend fun deleteDrafts(draftIds: List<Long>): Unit = withContext(Dispatchers.Main) {
        val controller = getLegacyController()
        if (controller != null) {
            try {
                val toDelete = controller.drafts.filter { draftIds.contains(it.draftId) }
                if (toDelete.isNotEmpty()) {
                    controller.delete(ArrayList(toDelete))
                }
            } catch (_: Throwable) {
                // ignore
            }
        }
        synchronized(lock) {
            draftIds.forEach { inMemoryDrafts.remove(it) }
            recomputeStateLocked()
        }
    }

    override suspend fun deleteForEdit(peerId: Long, storyId: Int): Unit = withContext(Dispatchers.Main) {
        val controller = getLegacyController()
        if (controller != null) {
            try {
                controller.deleteForEdit(peerId, storyId)
            } catch (_: Throwable) {
                // ignore
            }
        }
        synchronized(lock) {
            val iterator = inMemoryDrafts.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (entry.isEdit && entry.editStoryId == storyId && entry.editStoryPeerId == peerId) {
                    iterator.remove()
                }
            }
            recomputeStateLocked()
        }
    }

    override fun getDraftForEdit(peerId: Long, storyId: Int): StoryDraftModel? {
        val controller = getLegacyController()
        if (controller != null) {
            try {
                val legacy = controller.drafts.firstOrNull {
                    it.isEdit && it.editStoryId == storyId && it.editStoryPeerId == peerId
                }
                if (legacy != null) {
                    return DraftsMapper.toDomain(legacy)
                }
            } catch (_: Throwable) {
                // ignore
            }
        }
        synchronized(lock) {
            return inMemoryDrafts.values.firstOrNull {
                it.isEdit && it.editStoryId == storyId && it.editStoryPeerId == peerId
            }
        }
    }

    override suspend fun cleanupExpiredDrafts(now: Long, expirationPeriodMs: Long): List<Long> = withContext(Dispatchers.Main) {
        val expiredIds = mutableListOf<Long>()
        synchronized(lock) {
            val iterator = inMemoryDrafts.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (entry.isExpired(now, expirationPeriodMs)) {
                    expiredIds.add(entry.id)
                    iterator.remove()
                }
            }
            if (expiredIds.isNotEmpty()) {
                recomputeStateLocked()
            }
        }
        if (expiredIds.isNotEmpty()) {
            deleteDrafts(expiredIds)
        }
        expiredIds
    }

    private fun syncFromLegacy() {
        val controller = getLegacyController() ?: return
        val currentDrafts = ArrayList<StoryEntry>(controller.drafts)
        val domainList = currentDrafts.map { DraftsMapper.toDomain(it) }

        synchronized(lock) {
            inMemoryDrafts.clear()
            domainList.forEach { inMemoryDrafts[it.id] = it }
            recomputeStateLocked()
        }
    }

    private fun recomputeStateLocked() {
        val allDrafts = inMemoryDrafts.values.toList().sortedByDescending { it.date }
        _stateFlow.value = _stateFlow.value.copy(
            drafts = allDrafts,
            isLoading = false
        )
    }
}
