package org.telegram.messenger.feature.aitones.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel

/**
 * Contract for managing AI Compose tones.
 */
interface AiTonesRepository {
    fun observeTones(): Flow<AiTonesStateModel>
    fun getTonesState(): AiTonesStateModel
    suspend fun loadTones(force: Boolean = false): Result<AiTonesStateModel>
    suspend fun addTone(tone: AiToneModel): Result<Unit>
    suspend fun removeTone(tone: AiToneModel): Result<Unit>
    suspend fun unsaveTone(tone: AiToneModel): Result<Unit>
    suspend fun editTone(tone: AiToneModel): Result<Unit>
}
