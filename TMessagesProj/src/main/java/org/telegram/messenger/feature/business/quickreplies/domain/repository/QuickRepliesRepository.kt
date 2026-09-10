package org.telegram.messenger.feature.business.quickreplies.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel

interface QuickRepliesRepository {
    fun observeQuickReplies(): Flow<List<QuickReplyModel>>
    suspend fun getQuickReplies(): Result<List<QuickReplyModel>>
    suspend fun loadQuickReplies(force: Boolean = false): Result<Unit>
    suspend fun findReplyById(id: Int): Result<QuickReplyModel?>
    suspend fun findReplyByName(name: String): Result<QuickReplyModel?>
    suspend fun isNameBusy(name: String, exceptId: Int = -1): Result<Boolean>
    suspend fun canAddNew(): Result<Boolean>
    suspend fun renameReply(id: Int, newName: String): Result<Unit>
    suspend fun reorderReplies(ids: List<Int>): Result<Unit>
    suspend fun deleteReplies(ids: List<Int>): Result<Unit>
    suspend fun sendQuickReply(dialogId: Long, shortcutId: Int): Result<Unit>
}
