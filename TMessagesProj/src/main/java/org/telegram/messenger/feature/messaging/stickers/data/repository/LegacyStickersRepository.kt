package org.telegram.messenger.feature.messaging.stickers.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.data.mapper.StickerMapper
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

/**
 * Clean data adapter implementing [StickersRepository] backed by legacy [MediaDataController]
 * and [NotificationCenter], executing on [Dispatchers.Main].
 */
class LegacyStickersRepository(
    private val currentAccount: Int
) : StickersRepository {

    private val mediaDataController: MediaDataController
        get() = MediaDataController.getInstance(currentAccount)

    private val notificationCenter: NotificationCenter
        get() = NotificationCenter.getInstance(currentAccount)

    private fun readStickerSets(type: Int): List<StickerSetModel> {
        val rawSets = ArrayList(mediaDataController.getStickerSets(type))
        return StickerMapper.toSetDomainList(rawSets)
    }

    override fun observeStickerSets(type: Int): Flow<List<StickerSetModel>> = callbackFlow {
        fun emitCurrent() {
            trySend(readStickerSets(type))
        }

        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, args ->
            if (id == NotificationCenter.stickersDidLoad) {
                val loadedType = args?.getOrNull(0) as? Int
                if (loadedType == null || loadedType == type) {
                    emitCurrent()
                }
            } else if (id == NotificationCenter.recentDocumentsDidLoad) {
                emitCurrent()
            }
        }

        notificationCenter.addObserver(delegate, NotificationCenter.stickersDidLoad)
        notificationCenter.addObserver(delegate, NotificationCenter.recentDocumentsDidLoad)

        // Initial emission
        emitCurrent()

        awaitClose {
            notificationCenter.removeObserver(delegate, NotificationCenter.stickersDidLoad)
            notificationCenter.removeObserver(delegate, NotificationCenter.recentDocumentsDidLoad)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getStickerSets(type: Int): List<StickerSetModel> = withContext(Dispatchers.Main) {
        val sets = readStickerSets(type)
        if (sets.isEmpty()) {
            mediaDataController.loadStickers(type, true, false)
        }
        readStickerSets(type)
    }

    override suspend fun getStickerSet(id: Long): StickerSetModel? = withContext(Dispatchers.Main) {
        val set = mediaDataController.getStickerSetById(id)
        if (set != null) StickerMapper.toDomain(set) else null
    }

    override suspend fun getRecentStickers(type: Int): List<StickerModel> = withContext(Dispatchers.Main) {
        val recent = mediaDataController.getRecentStickers(type)
        StickerMapper.toDomainList(recent)
    }

    override suspend fun getStickersForEmoji(emoji: String): List<StickerModel> = withContext(Dispatchers.Main) {
        val all = mediaDataController.allStickers
        val cleanEmoji = emoji.trim()
        val list = all[cleanEmoji]
        StickerMapper.toDomainList(list)
    }

    override suspend fun toggleStickerSetInstalled(id: Long, install: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val set = mediaDataController.getStickerSetById(id)
            if (set != null) {
                val toggle = if (install) 2 else 0
                mediaDataController.toggleStickerSet(
                    ApplicationLoader.applicationContext,
                    set,
                    toggle,
                    null,
                    false,
                    false
                )
                Result.success(Unit)
            } else {
                Result.failure(AppError.NotFound("Sticker set with id $id not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to toggle sticker set", e))
        }
    }

    override suspend fun toggleStickerSetArchived(id: Long, archive: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val set = mediaDataController.getStickerSetById(id)
            if (set != null) {
                val toggle = if (archive) 1 else 2
                mediaDataController.toggleStickerSet(
                    ApplicationLoader.applicationContext,
                    set,
                    toggle,
                    null,
                    false,
                    false
                )
                Result.success(Unit)
            } else {
                Result.failure(AppError.NotFound("Sticker set with id $id not found"))
            }
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to toggle archive on sticker set", e))
        }
    }
}
