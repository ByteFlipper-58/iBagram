package org.telegram.messenger.feature.drafts.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.drafts.domain.model.StoryDraftModel

/**
 * Repository interface for story and media drafts storage,
 * retrieval, editing, and expiration cleanup.
 */
interface DraftsRepository {
    fun observeDraftsState(): Flow<DraftsStateModel>
    fun getDraftsState(): DraftsStateModel

    suspend fun loadDrafts(force: Boolean = false)
    suspend fun saveDraft(draft: StoryDraftModel)
    suspend fun deleteDraft(draftId: Long)
    suspend fun deleteDrafts(draftIds: List<Long>)

    suspend fun deleteForEdit(peerId: Long, storyId: Int)
    fun getDraftForEdit(peerId: Long, storyId: Int): StoryDraftModel?

    suspend fun cleanupExpiredDrafts(now: Long, expirationPeriodMs: Long = 7L * 24 * 3600 * 1000L): List<Long>
}
