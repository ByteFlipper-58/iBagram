package org.telegram.messenger.feature.messaging.dialogs.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.dialogs.data.datasource.DialogsLocalDataSource
import org.telegram.messenger.feature.messaging.dialogs.data.datasource.DialogsRemoteDataSource
import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository

/**
 * Чистая реализация [DialogsRepository], объединяющая локальный кэш диалогов и удаленные вызовы.
 */
class DialogsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: DialogsLocalDataSource,
    private val remoteDataSource: DialogsRemoteDataSource
) : DialogsRepository {

    override fun getDialogs(folderId: Int): Flow<List<DialogModel>> =
        localDataSource.observeDialogs(folderId)

    override suspend fun loadMoreDialogs(folderId: Int, count: Int): Result<Unit> {
        val current = localDataSource.getDialogs(folderId)
        return remoteDataSource.loadMoreDialogs(folderId, current.size, count)
    }

    override suspend fun pinDialog(dialogId: Long, pin: Boolean): Result<Unit> {
        localDataSource.pinDialog(dialogId, pin)
        return remoteDataSource.pinDialog(dialogId, pin)
    }

    override suspend fun deleteDialog(dialogId: Long, revoke: Boolean): Result<Unit> {
        localDataSource.deleteDialog(dialogId)
        return remoteDataSource.deleteDialog(dialogId, revoke)
    }

    override suspend fun markAsRead(dialogId: Long): Result<Unit> {
        localDataSource.markAsRead(dialogId)
        return remoteDataSource.markAsRead(dialogId)
    }
}
