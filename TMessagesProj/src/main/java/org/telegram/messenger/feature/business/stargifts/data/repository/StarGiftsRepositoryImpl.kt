package org.telegram.messenger.feature.business.stargifts.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.data.datasource.StarGiftsLocalDataSource
import org.telegram.messenger.feature.business.stargifts.data.datasource.StarGiftsRemoteDataSource
import org.telegram.messenger.feature.business.stargifts.data.mapper.StarGiftMapper
import org.telegram.messenger.feature.business.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftsCatalogModel
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository

/**
 * Repository implementation coordinating StarGiftsLocalDataSource and StarGiftsRemoteDataSource.
 */
class StarGiftsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: StarGiftsLocalDataSource,
    private val remoteDataSource: StarGiftsRemoteDataSource,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StarGiftsRepository {

    override fun observeCatalog(): Flow<StarGiftsCatalogModel> {
        return localDataSource.observeCatalogUpdated()
            .map {
                val gifts = localDataSource.getCatalog()
                StarGiftMapper.mapCatalog(gifts, isLoading = false)
            }
            .onStart {
                val gifts = localDataSource.getCatalog()
                emit(StarGiftMapper.mapCatalog(gifts, isLoading = gifts.isEmpty()))
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun getCatalog(forceRefresh: Boolean): Result<List<StarGiftModel>> = withContext(defaultDispatcher) {
        val cached = localDataSource.getCatalog()
        if (!forceRefresh && cached.isNotEmpty()) {
            return@withContext Result.Success(StarGiftMapper.mapStarGiftList(cached))
        }

        when (val res = remoteDataSource.fetchCatalog(forceRefresh)) {
            is Result.Success -> {
                localDataSource.saveCatalog(res.data)
                Result.Success(StarGiftMapper.mapStarGiftList(res.data))
            }
            is Result.Failure -> {
                if (cached.isNotEmpty()) {
                    Result.Success(StarGiftMapper.mapStarGiftList(cached))
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun getGift(giftId: Long): Result<StarGiftModel?> = withContext(defaultDispatcher) {
        val cached = localDataSource.getGift(giftId)
        if (cached != null) {
            return@withContext Result.Success(StarGiftMapper.mapStarGift(cached))
        }

        when (val res = remoteDataSource.fetchGift(giftId)) {
            is Result.Success -> {
                res.data?.let { localDataSource.saveGift(it) }
                Result.Success(StarGiftMapper.mapStarGift(res.data))
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override fun observeProfileGifts(dialogId: Long): Flow<ProfileGiftsModel> {
        return localDataSource.observeProfileGiftsUpdated()
            .map { updatedDid ->
                val gifts = localDataSource.getProfileGifts(dialogId)
                StarGiftMapper.mapProfileGifts(
                    dialogId = dialogId,
                    savedGifts = gifts,
                    totalCount = gifts.size,
                    hasMore = false,
                    nextOffset = null,
                    isLoading = false
                )
            }
            .onStart {
                val gifts = localDataSource.getProfileGifts(dialogId)
                emit(
                    StarGiftMapper.mapProfileGifts(
                        dialogId = dialogId,
                        savedGifts = gifts,
                        totalCount = gifts.size,
                        hasMore = false,
                        nextOffset = null,
                        isLoading = gifts.isEmpty()
                    )
                )
            }
            .flowOn(defaultDispatcher)
    }

    override suspend fun loadProfileGifts(
        dialogId: Long,
        offset: String?,
        limit: Int,
        filter: StarGiftFilter
    ): Result<ProfileGiftsModel> = withContext(defaultDispatcher) {
        when (val res = remoteDataSource.fetchProfileGifts(dialogId, offset, limit, filter)) {
            is Result.Success -> {
                val data = res.data
                localDataSource.saveProfileGifts(dialogId, data.gifts ?: emptyList())
                val hasMore = (data.gifts?.size ?: 0) >= limit && data.next_offset != null
                val model = StarGiftMapper.mapProfileGifts(
                    dialogId = dialogId,
                    savedGifts = data.gifts,
                    totalCount = data.count,
                    hasMore = hasMore,
                    nextOffset = data.next_offset,
                    isLoading = false
                )
                Result.Success(model)
            }
            is Result.Failure -> {
                val cached = localDataSource.getProfileGifts(dialogId)
                if (cached.isNotEmpty()) {
                    Result.Success(
                        StarGiftMapper.mapProfileGifts(
                            dialogId = dialogId,
                            savedGifts = cached,
                            totalCount = cached.size,
                            hasMore = false,
                            nextOffset = null,
                            isLoading = false
                        )
                    )
                } else {
                    Result.Failure(res.error)
                }
            }
        }
    }

    override suspend fun togglePinGift(
        dialogId: Long,
        giftId: Long,
        pin: Boolean
    ): Result<Boolean> = withContext(defaultDispatcher) {
        val localSuccess = localDataSource.togglePin(dialogId, giftId, pin)
        if (localSuccess) {
            return@withContext Result.Success(true)
        }
        remoteDataSource.togglePinGift(dialogId, giftId, pin)
    }

    override suspend fun toggleHideGift(
        dialogId: Long,
        giftId: Long,
        hide: Boolean
    ): Result<Boolean> = withContext(defaultDispatcher) {
        val localSuccess = localDataSource.toggleHide(dialogId, giftId, hide)
        if (localSuccess) {
            return@withContext Result.Success(true)
        }
        remoteDataSource.toggleHideGift(dialogId, giftId, hide)
    }
}
