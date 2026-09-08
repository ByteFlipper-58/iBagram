package org.telegram.messenger.feature.stickers.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.stickers.domain.model.StickerSetModel

/**
 * Clean domain repository contract for stickers, sticker packs, and emojis.
 */
interface StickersRepository {
    fun observeStickerSets(type: Int = 0): Flow<List<StickerSetModel>>
    suspend fun getStickerSets(type: Int = 0): List<StickerSetModel>
    suspend fun getStickerSet(id: Long): StickerSetModel?
    suspend fun getRecentStickers(type: Int = 0): List<StickerModel>
    suspend fun getStickersForEmoji(emoji: String): List<StickerModel>
    suspend fun toggleStickerSetInstalled(id: Long, install: Boolean): Result<Unit>
    suspend fun toggleStickerSetArchived(id: Long, archive: Boolean): Result<Unit>
}
