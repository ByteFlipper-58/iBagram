package org.telegram.messenger.feature.business.quickreplies.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.data.datasource.QuickRepliesLocalDataSource
import org.telegram.messenger.feature.business.quickreplies.data.datasource.QuickRepliesRemoteDataSource
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository

class QuickRepliesRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: QuickRepliesLocalDataSource,
    private val remoteDataSource: QuickRepliesRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : QuickRepliesRepository {

    override fun observeQuickReplies(): Flow<List<QuickReplyModel>> {
        return localDataSource.observeQuickRepliesUpdated()
            .onStart {
                localDataSource.load()
                emit(Unit)
            }
            .map { localDataSource.getReplies() }
            .flowOn(mainDispatcher)
            .distinctUntilChanged()
    }

    override suspend fun getQuickReplies(): Result<List<QuickReplyModel>> = withContext(mainDispatcher) {
        try {
            Result.Success(localDataSource.getReplies())
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get quick replies: ${e.message}", e))
        }
    }

    override suspend fun loadQuickReplies(force: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.load()
            if (force) {
                remoteDataSource.loadQuickReplies()
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to load quick replies: ${e.message}", e))
        }
    }

    override suspend fun findReplyById(id: Int): Result<QuickReplyModel?> = withContext(mainDispatcher) {
        try {
            Result.Success(localDataSource.findReplyById(id))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to find reply by id $id: ${e.message}", e))
        }
    }

    override suspend fun findReplyByName(name: String): Result<QuickReplyModel?> = withContext(mainDispatcher) {
        try {
            Result.Success(localDataSource.findReplyByName(name))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to find reply by name $name: ${e.message}", e))
        }
    }

    override suspend fun isNameBusy(name: String, exceptId: Int): Result<Boolean> = withContext(mainDispatcher) {
        try {
            Result.Success(localDataSource.isNameBusy(name, exceptId))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check if name is busy: ${e.message}", e))
        }
    }

    override suspend fun canAddNew(): Result<Boolean> = withContext(mainDispatcher) {
        try {
            Result.Success(localDataSource.canAddNew())
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check if new reply can be added: ${e.message}", e))
        }
    }

    override suspend fun renameReply(id: Int, newName: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.renameReply(id, newName)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to rename reply $id: ${e.message}", e))
        }
    }

    override suspend fun reorderReplies(ids: List<Int>): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.reorderReplies(ids)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to reorder replies: ${e.message}", e))
        }
    }

    override suspend fun deleteReplies(ids: List<Int>): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.deleteReplies(ids)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to delete replies: ${e.message}", e))
        }
    }

    override suspend fun sendQuickReply(dialogId: Long, shortcutId: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            val reply = localDataSource.findReplyById(shortcutId)
                ?: return@withContext Result.Failure(AppError.NotFound("Quick reply with id $shortcutId not found"))
            localDataSource.sendQuickReply(dialogId, shortcutId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to send quick reply: ${e.message}", e))
        }
    }
}
