package org.telegram.messenger.feature.messaging.aitones.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.aitones.data.datasource.AiTonesLocalDataSource
import org.telegram.messenger.feature.messaging.aitones.data.datasource.AiTonesRemoteDataSource
import org.telegram.messenger.feature.messaging.aitones.data.mapper.AiToneMapper
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.messaging.aitones.domain.repository.AiTonesRepository
import org.telegram.tgnet.tl.TL_aicompose

/**
 * Modern repository implementation for AI Compose tones, coordinating remote MTProto requests and local storage.
 */
class AiTonesRepositoryImpl(
    private val currentAccount: Int,
    private val remoteDataSource: AiTonesRemoteDataSource,
    private val localDataSource: AiTonesLocalDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : AiTonesRepository {

    override fun observeTones(): Flow<AiTonesStateModel> {
        return NotificationCenterFlowBridge.observeEvent(
            account = currentAccount,
            eventId = NotificationCenter.loadedAiComposeTones
        )
            .map {
                getTonesState()
            }
            .onStart {
                emit(getTonesState())
            }
            .flowOn(mainDispatcher)
    }

    override fun getTonesState(): AiTonesStateModel {
        val currentTones = localDataSource.getCurrentTones()
        val mapped = currentTones.mapNotNull { AiToneMapper.mapTone(it) }
        return AiTonesStateModel(
            tones = mapped,
            savedCount = mapped.count { it.isCreator },
            isLoading = false,
            hash = localDataSource.getCurrentHash()
        )
    }

    override suspend fun loadTones(force: Boolean): Result<AiTonesStateModel> =
        withContext(mainDispatcher) {
            val (cachedHash, cachedTones) = localDataSource.getLocalTones()
            val requestHash = if (force) 0L else cachedHash

            val remoteResult = remoteDataSource.getTones(requestHash)
            when (remoteResult) {
                is Result.Success -> {
                    val response = remoteResult.data
                    if (response is TL_aicompose.TL_tones) {
                        localDataSource.saveLocalTones(response.hash, response.tones)
                        postNotificationSafely(NotificationCenter.loadedAiComposeTones)
                    }
                    Result.Success(getTonesState())
                }
                is Result.Failure -> {
                    if (cachedTones.isNotEmpty()) {
                        Result.Success(getTonesState())
                    } else {
                        Result.Failure(remoteResult.error)
                    }
                }
            }
        }

    override suspend fun addTone(tone: AiToneModel): Result<Unit> =
        withContext(mainDispatcher) {
            val newTone = TL_aicompose.TL_aiComposeTone().apply {
                this.id = tone.id ?: System.currentTimeMillis()
                this.title = tone.title
                this.emoji_id = tone.emojiDocumentId ?: 0L
                this.slug = tone.slug ?: ""
                this.prompt = tone.prompt ?: ""
                this.creator = tone.isCreator
                this.installs_count = tone.installsCount
            }
            localDataSource.addTone(newTone)
            postNotificationSafely(NotificationCenter.loadedAiComposeTones)
            Result.Success(Unit)
        }

    override suspend fun removeTone(tone: AiToneModel): Result<Unit> =
        withContext(mainDispatcher) {
            val toneId = tone.id ?: return@withContext Result.Failure(AppError.InvalidInput("Tone ID cannot be null"))
            val removed = localDataSource.removeTone(toneId)
            if (removed) {
                postNotificationSafely(NotificationCenter.loadedAiComposeTones)
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.NotFound("Tone not found in local cache"))
            }
        }

    override suspend fun unsaveTone(tone: AiToneModel): Result<Unit> =
        withContext(mainDispatcher) {
            val toneId = tone.id ?: return@withContext Result.Failure(AppError.InvalidInput("Tone ID cannot be null"))
            val current = localDataSource.getCurrentTones()
            val matching = current.firstOrNull { it is TL_aicompose.TL_aiComposeTone && it.id == toneId }

            localDataSource.removeTone(toneId)
            postNotificationSafely(NotificationCenter.loadedAiComposeTones)

            if (matching != null) {
                remoteDataSource.unsaveTone(matching)
            }
            Result.Success(Unit)
        }

    override suspend fun editTone(tone: AiToneModel): Result<Unit> =
        withContext(mainDispatcher) {
            val toneId = tone.id ?: return@withContext Result.Failure(AppError.InvalidInput("Tone ID cannot be null"))
            val current = localDataSource.getCurrentTones()
            val existing = current.firstOrNull { it is TL_aicompose.TL_aiComposeTone && it.id == toneId }

            if (existing is TL_aicompose.TL_aiComposeTone) {
                val updated = TL_aicompose.TL_aiComposeTone().apply {
                    this.id = existing.id
                    this.title = tone.title
                    this.prompt = tone.prompt ?: existing.prompt
                    this.emoji_id = tone.emojiDocumentId ?: existing.emoji_id
                    this.slug = existing.slug
                    this.creator = existing.creator
                    this.installs_count = existing.installs_count
                }
                localDataSource.editTone(updated)
                postNotificationSafely(NotificationCenter.loadedAiComposeTones)
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.NotFound("Tone not found to edit"))
            }
        }

    private fun postNotificationSafely(id: Int) {
        try {
            NotificationCenter.getInstance(currentAccount)?.postNotificationName(id)
        } catch (_: Throwable) {
            // Ignored in headless unit test environment
        }
    }
}
