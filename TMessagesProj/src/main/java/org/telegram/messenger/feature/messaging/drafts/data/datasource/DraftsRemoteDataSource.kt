package org.telegram.messenger.feature.messaging.drafts.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel

class DraftsRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun syncRemoteDrafts(): Result<List<StoryDraftModel>> {
        return Result.Success(emptyList())
    }

    suspend fun pushDraftToServer(draft: StoryDraftModel): Result<Unit> {
        return Result.Success(Unit)
    }

    suspend fun deleteRemoteDraft(draftId: Long): Result<Unit> {
        return Result.Success(Unit)
    }
}
