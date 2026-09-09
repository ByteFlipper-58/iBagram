package org.telegram.messenger.feature.aitones.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.telegram.messenger.AiTonesController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.aitones.data.mapper.AiToneMapper
import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository
import org.telegram.tgnet.tl.TL_aicompose

class LegacyAiTonesRepository(
    private val currentAccount: Int
) : AiTonesRepository {

    private fun getController(): AiTonesController {
        val mc = MessagesController.getInstance(currentAccount)
        return mc.tonesController ?: mc.getTonesController()
    }

    override fun observeTones(): Flow<AiTonesStateModel> {
        return NotificationCenterFlowBridge.observeEvent(
            account = currentAccount,
            eventId = NotificationCenter.loadedAiComposeTones
        ).map {
            getTonesState()
        }
    }

    override fun getTonesState(): AiTonesStateModel {
        val controller = getController()
        return AiToneMapper.mapState(controller)
    }

    override suspend fun loadTones(force: Boolean): Result<AiTonesStateModel> =
        withContext(Dispatchers.Main) {
            try {
                val controller = getController()
                if (force) {
                    controller.invalidate()
                } else {
                    controller.load()
                }
                Result.Success(AiToneMapper.mapState(controller))
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to load AI compose tones", e))
            }
        }

    override suspend fun addTone(tone: AiToneModel): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val controller = getController()
                val existing = AiToneMapper.findMatchingTone(controller, tone)
                if (existing != null) {
                    controller.add(existing)
                } else {
                    val newTone = TL_aicompose.TL_aiComposeTone().apply {
                        this.id = tone.id ?: System.currentTimeMillis()
                        this.title = tone.title
                        this.emoji_id = tone.emojiDocumentId ?: 0L
                        this.slug = tone.slug ?: ""
                        this.prompt = tone.prompt ?: ""
                        this.creator = tone.isCreator
                        this.installs_count = tone.installsCount
                    }
                    controller.add(newTone)
                }
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to add AI compose tone", e))
            }
        }

    override suspend fun removeTone(tone: AiToneModel): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val controller = getController()
                val existing = AiToneMapper.findMatchingTone(controller, tone)
                if (existing != null) {
                    controller.remove(existing)
                    Result.Success(Unit)
                } else {
                    Result.Failure(AppError.NotFound("Tone not found in local cache"))
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to remove AI compose tone", e))
            }
        }

    override suspend fun unsaveTone(tone: AiToneModel): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val controller = getController()
                val existing = AiToneMapper.findMatchingTone(controller, tone)
                if (existing != null) {
                    controller.unsave(existing)
                    Result.Success(Unit)
                } else {
                    Result.Failure(AppError.NotFound("Tone not found in local cache"))
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to unsave AI compose tone", e))
            }
        }

    override suspend fun editTone(tone: AiToneModel): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                val controller = getController()
                val existing = AiToneMapper.findMatchingTone(controller, tone)
                if (existing is TL_aicompose.TL_aiComposeTone) {
                    existing.title = tone.title
                    existing.prompt = tone.prompt ?: existing.prompt
                    existing.emoji_id = tone.emojiDocumentId ?: existing.emoji_id
                    controller.edit(existing)
                    Result.Success(Unit)
                } else {
                    Result.Failure(AppError.InvalidInput("Cannot edit tone: not a custom AI tone"))
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic(e.message ?: "Failed to edit AI compose tone", e))
            }
        }
}
