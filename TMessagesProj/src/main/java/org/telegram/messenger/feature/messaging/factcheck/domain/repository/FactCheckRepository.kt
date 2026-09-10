package org.telegram.messenger.feature.messaging.factcheck.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckModel

interface FactCheckRepository {
    fun observeFactCheckLoaded(): Flow<Unit>
    suspend fun getFactCheck(dialogId: Long, messageId: Int, hash: Long): FactCheckModel?
    suspend fun loadFactCheck(dialogId: Long, messageId: Int): Result<FactCheckModel?>
    suspend fun applyFactCheck(
        dialogId: Long,
        messageId: Int,
        text: String,
        entities: List<FactCheckEntityModel>? = null
    ): Result<Unit>
    suspend fun deleteFactCheck(dialogId: Long, messageId: Int): Result<Unit>
    suspend fun getFactCheckLimit(): Int
}
