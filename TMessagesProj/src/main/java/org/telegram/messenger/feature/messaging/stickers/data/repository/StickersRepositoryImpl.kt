package org.telegram.messenger.feature.messaging.stickers.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.stickers.data.datasource.StickersLocalDataSource
import org.telegram.messenger.feature.messaging.stickers.data.datasource.StickersRemoteDataSource
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository

/**
 * Чистая реализация [StickersRepository], координирующая кэш стикеров и удаленные RPC-вызовы.
 */
class StickersRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: StickersLocalDataSource,
    private val remoteDataSource: StickersRemoteDataSource
) : StickersRepository {

    override fun observeStickerSets(type: Int): Flow<List<StickerSetModel>> =
        localDataSource.observeStickerSets(type)

    override suspend fun getStickerSets(type: Int): List<StickerSetModel> {
        val cached = localDataSource.getStickerSets(type)
        if (cached.isNotEmpty()) return cached

        val remote = remoteDataSource.loadStickerSets(type, false)
        if (remote is Result.Success && remote.data.isNotEmpty()) {
            localDataSource.setStickerSets(type, remote.data)
            return remote.data
        }
        return cached
    }

    override suspend fun getStickerSet(id: Long): StickerSetModel? =
        localDataSource.getStickerSet(id)

    override suspend fun getRecentStickers(type: Int): List<StickerModel> =
        localDataSource.getRecentStickers(type)

    override suspend fun getStickersForEmoji(emoji: String): List<StickerModel> =
        localDataSource.getStickersForEmoji(emoji)

    override suspend fun toggleStickerSetInstalled(id: Long, install: Boolean): Result<Unit> =
        remoteDataSource.toggleStickerSetInstalled(id, install)

    override suspend fun toggleStickerSetArchived(id: Long, archive: Boolean): Result<Unit> =
        remoteDataSource.toggleStickerSetArchived(id, archive)
}
