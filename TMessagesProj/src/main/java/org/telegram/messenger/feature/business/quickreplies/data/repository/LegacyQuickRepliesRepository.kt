package org.telegram.messenger.feature.business.quickreplies.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.events.NotificationEvent
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.data.mapper.QuickReplyMapper
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository
import org.telegram.ui.Business.QuickRepliesController

class LegacyQuickRepliesRepository(
    private val currentAccount: Int,
    private val controllerProvider: () -> QuickRepliesController = {
        QuickRepliesController.getInstance(currentAccount)
    }
) : QuickRepliesRepository {

    override fun observeQuickReplies(): Flow<List<QuickReplyModel>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.quickRepliesUpdated)
            .onStart {
                loadQuickReplies(false)
                emit(NotificationEvent(NotificationCenter.quickRepliesUpdated, currentAccount, emptyArray()))
            }
            .map {
                val controller = controllerProvider()
                QuickReplyMapper.mapToQuickReplyList(controller.replies)
            }
            .flowOn(Dispatchers.Main)
            .distinctUntilChanged()
    }

    override suspend fun getQuickReplies(): Result<List<QuickReplyModel>> = withContext(Dispatchers.Main) {
        try {
            val controller = controllerProvider()
            Result.Success(QuickReplyMapper.mapToQuickReplyList(controller.replies))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to get quick replies: ${e.message}", e))
        }
    }

    override suspend fun loadQuickReplies(force: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            controllerProvider().load()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to load quick replies: ${e.message}", e))
        }
    }

    override suspend fun findReplyById(id: Int): Result<QuickReplyModel?> = withContext(Dispatchers.Main) {
        try {
            val reply = controllerProvider().findReply(id.toLong())
            Result.Success(reply?.let { QuickReplyMapper.mapToQuickReply(it) })
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to find reply by id $id: ${e.message}", e))
        }
    }

    override suspend fun findReplyByName(name: String): Result<QuickReplyModel?> = withContext(Dispatchers.Main) {
        try {
            val reply = controllerProvider().findReply(name)
            Result.Success(reply?.let { QuickReplyMapper.mapToQuickReply(it) })
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to find reply by name $name: ${e.message}", e))
        }
    }

    override suspend fun isNameBusy(name: String, exceptId: Int): Result<Boolean> = withContext(Dispatchers.Main) {
        try {
            Result.Success(controllerProvider().isNameBusy(name, exceptId))
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check if name is busy: ${e.message}", e))
        }
    }

    override suspend fun canAddNew(): Result<Boolean> = withContext(Dispatchers.Main) {
        try {
            Result.Success(controllerProvider().canAddNew())
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to check if new reply can be added: ${e.message}", e))
        }
    }

    override suspend fun renameReply(id: Int, newName: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            controllerProvider().renameReply(id, newName)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to rename reply $id: ${e.message}", e))
        }
    }

    override suspend fun reorderReplies(ids: List<Int>): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val controller = controllerProvider()
            for (i in ids.indices) {
                val reply = controller.findReply(ids[i].toLong())
                if (reply != null) {
                    reply.order = i
                }
            }
            controller.reorder()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to reorder replies: ${e.message}", e))
        }
    }

    override suspend fun deleteReplies(ids: List<Int>): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            controllerProvider().deleteReplies(ArrayList(ids))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to delete replies: ${e.message}", e))
        }
    }

    override suspend fun sendQuickReply(dialogId: Long, shortcutId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val controller = controllerProvider()
            val reply = controller.findReply(shortcutId.toLong())
                ?: return@withContext Result.Failure(AppError.NotFound("Quick reply with id $shortcutId not found"))
            controller.sendQuickReplyTo(dialogId, reply)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to send quick reply: ${e.message}", e))
        }
    }
}
