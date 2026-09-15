package org.telegram.messenger.feature.messaging.mentions.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.mentions.data.datasource.MentionsLocalDataSource
import org.telegram.messenger.feature.messaging.mentions.data.datasource.MentionsRemoteDataSource
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionsState
import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository

/**
 * Чистая реализация [MentionsRepository], координирующая локальный и удаленный источники данных.
 */
class MentionsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: MentionsLocalDataSource,
    private val remoteDataSource: MentionsRemoteDataSource
) : MentionsRepository {

    override fun observeState(): Flow<MentionsState> = localDataSource.observeState()

    override fun getState(): MentionsState = localDataSource.getState()

    override fun updateQuery(query: MentionQuery) {
        localDataSource.updateQuery(query)
    }

    override fun setCandidates(candidates: List<MentionCandidate>) {
        localDataSource.setCandidates(candidates)
    }

    override fun setSearching(isSearching: Boolean) {
        localDataSource.setSearching(isSearching)
    }

    override fun setPanelVisible(isVisible: Boolean) {
        localDataSource.setPanelVisible(isVisible)
    }

    override fun clear() {
        localDataSource.clear()
    }
}
