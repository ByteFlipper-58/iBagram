package org.telegram.messenger.feature.messaging.drafts.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.drafts.data.datasource.DraftsLocalDataSource
import org.telegram.messenger.feature.messaging.drafts.data.datasource.DraftsRemoteDataSource
import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftsStateModel
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.messaging.drafts.domain.repository.DraftsRepository

class DraftsRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: DraftsLocalDataSource,
    private val remoteDataSource: DraftsRemoteDataSource
) : DraftsRepository {

    override fun observeDraftsState(): Flow<DraftsStateModel> = localDataSource.stateFlow

    override fun getDraftsState(): DraftsStateModel = localDataSource.getState()

    override suspend fun loadDrafts(force: Boolean) {
        if (force) {
            localDataSource.setLoading(true)
            val remoteResult = remoteDataSource.syncRemoteDrafts()
            if (remoteResult is Result.Success && remoteResult.data.isNotEmpty()) {
                localDataSource.setDrafts(remoteResult.data)
            } else {
                localDataSource.setLoading(false)
            }
        }
    }

    override suspend fun saveDraft(draft: StoryDraftModel) {
        localDataSource.saveDraft(draft)
        remoteDataSource.pushDraftToServer(draft)
    }

    override suspend fun deleteDraft(draftId: Long) {
        localDataSource.deleteDraft(draftId)
        remoteDataSource.deleteRemoteDraft(draftId)
    }

    override suspend fun deleteDrafts(draftIds: List<Long>) {
        localDataSource.deleteDrafts(draftIds)
        for (id in draftIds) {
            remoteDataSource.deleteRemoteDraft(id)
        }
    }

    override suspend fun deleteForEdit(peerId: Long, storyId: Int) {
        localDataSource.deleteForEdit(peerId, storyId)
    }

    override fun getDraftForEdit(peerId: Long, storyId: Int): StoryDraftModel? {
        return localDataSource.getDraftForEdit(peerId, storyId)
    }

    override suspend fun cleanupExpiredDrafts(now: Long, expirationPeriodMs: Long): List<Long> {
        return localDataSource.cleanupExpired(now, expirationPeriodMs)
    }
}
