package org.telegram.messenger.feature.messaging.stickers.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.data.mapper.StickerMapper
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel

/**
 * Удаленный источник данных для сетевых операций со стикерами через MediaDataController.
 */
class StickersRemoteDataSource(
    private val currentAccount: Int
) {

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    private val mediaDataController: MediaDataController?
        get() = try {
            if (isLegacyAvailable) MediaDataController.getInstance(currentAccount) else null
        } catch (_: Throwable) {
            null
        }

    suspend fun loadStickerSets(type: Int, force: Boolean): Result<List<StickerSetModel>> {
        if (!isLegacyAvailable) return Result.success(emptyList())
        val controller = mediaDataController ?: return Result.failure(AppError.NotFound("MediaDataController not available"))
        return try {
            controller.loadStickers(type, true, false)
            val sets = controller.getStickerSets(type)
            val mapped = StickerMapper.toSetDomainList(if (sets != null) ArrayList(sets) else ArrayList())
            Result.success(mapped)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to load stickers", e))
        }
    }

    suspend fun toggleStickerSetInstalled(id: Long, install: Boolean): Result<Unit> {
        if (!isLegacyAvailable) return Result.failure(AppError.NotFound("Sticker set with id $id not found"))
        val controller = mediaDataController ?: return Result.failure(AppError.NotFound("MediaDataController not available"))
        return try {
            val set = controller.getStickerSetById(id)
            if (set != null) {
                val toggle = if (install) 2 else 0
                controller.toggleStickerSet(
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

    suspend fun toggleStickerSetArchived(id: Long, archive: Boolean): Result<Unit> {
        if (!isLegacyAvailable) return Result.failure(AppError.NotFound("Sticker set with id $id not found"))
        val controller = mediaDataController ?: return Result.failure(AppError.NotFound("MediaDataController not available"))
        return try {
            val set = controller.getStickerSetById(id)
            if (set != null) {
                val toggle = if (archive) 1 else 2
                controller.toggleStickerSet(
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
