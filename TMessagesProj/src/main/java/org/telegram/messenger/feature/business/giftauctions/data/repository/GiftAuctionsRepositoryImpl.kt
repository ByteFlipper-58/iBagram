package org.telegram.messenger.feature.business.giftauctions.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.GiftAuctionController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.data.datasource.GiftAuctionsLocalDataSource
import org.telegram.messenger.feature.business.giftauctions.data.datasource.GiftAuctionsRemoteDataSource
import org.telegram.messenger.feature.business.giftauctions.data.mapper.GiftAuctionMapper
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository
import kotlin.coroutines.resume

class GiftAuctionsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: GiftAuctionsLocalDataSource,
    private val remoteDataSource: GiftAuctionsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : GiftAuctionsRepository {

    override fun observeActiveAuctions(): Flow<List<GiftAuctionModel>> = callbackFlow {
        val listener = GiftAuctionController.OnActiveAuctionsUpdateListeners { auctions ->
            val models = auctions?.mapNotNull { GiftAuctionMapper.mapAuction(it) } ?: emptyList()
            trySend(models)
        }

        localDataSource.subscribeToActiveAuctions(listener)
        val current = localDataSource.getActiveAuctions().mapNotNull { GiftAuctionMapper.mapAuction(it) }
        trySend(current)

        awaitClose {
            localDataSource.unsubscribeFromActiveAuctions(listener)
        }
    }.flowOn(mainDispatcher)

    override fun observeAuction(giftId: Long): Flow<GiftAuctionModel?> = callbackFlow {
        val listener = GiftAuctionController.OnAuctionUpdateListener { auction ->
            trySend(GiftAuctionMapper.mapAuction(auction))
        }

        val initial = localDataSource.subscribeToAuction(giftId, listener)
        if (initial != null) {
            trySend(GiftAuctionMapper.mapAuction(initial))
        }

        awaitClose {
            localDataSource.unsubscribeFromAuction(giftId, listener)
        }
    }.flowOn(mainDispatcher)

    override suspend fun getActiveAuctions(): List<GiftAuctionModel> = withContext(mainDispatcher) {
        localDataSource.getActiveAuctions().mapNotNull { GiftAuctionMapper.mapAuction(it) }
    }

    override suspend fun getAuctionById(giftId: Long): Result<GiftAuctionModel> = withContext(mainDispatcher) {
        val cached = localDataSource.getAuction(giftId)
        if (cached != null) {
            val model = GiftAuctionMapper.mapAuction(cached)
            if (model != null) {
                return@withContext Result.Success(model)
            }
        }

        when (val res = remoteDataSource.getAuctionById(giftId)) {
            is Result.Success -> {
                val auction = localDataSource.getAuction(giftId)
                val model = GiftAuctionMapper.mapAuction(auction)
                if (model != null) {
                    Result.Success(model)
                } else {
                    Result.Failure(AppError.NotFound("Auction model could not be mapped"))
                }
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override suspend fun getAuctionBySlug(slug: String): Result<GiftAuctionModel> = withContext(mainDispatcher) {
        when (val res = remoteDataSource.getAuctionBySlug(slug)) {
            is Result.Success -> {
                val state = res.data
                val giftId = state.gift?.id ?: return@withContext Result.Failure(AppError.NotFound("Auction gift not found in slug response"))
                val auction = localDataSource.getAuction(giftId)
                val model = GiftAuctionMapper.mapAuction(auction)
                if (model != null) {
                    Result.Success(model)
                } else {
                    Result.Failure(AppError.NotFound("Auction model could not be mapped"))
                }
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override suspend fun sendBid(
        giftId: Long,
        amount: Long,
        params: GiftAuctionBidParamsModel?
    ): Result<Unit> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val legacyParams = GiftAuctionMapper.toLegacyBidParams(params)
            localDataSource.sendBid(giftId, legacyParams, amount) { success, error ->
                if (success) {
                    cont.resume(Result.Success(Unit))
                } else {
                    cont.resume(Result.Failure(AppError.Network(error ?: "Failed to send bid", -1)))
                }
            }
        }
    }

    override suspend fun loadAcquiredGifts(giftId: Long): Result<List<GiftAuctionAcquiredGiftModel>> = withContext(mainDispatcher) {
        when (val res = remoteDataSource.loadAcquiredGifts(giftId)) {
            is Result.Success -> {
                val mapped = res.data.mapNotNull { GiftAuctionMapper.mapAcquiredGift(giftId, it) }
                Result.Success(mapped)
            }
            is Result.Failure -> Result.Failure(res.error)
        }
    }

    override suspend fun refreshActiveAuctions(): Result<Unit> = withContext(mainDispatcher) {
        remoteDataSource.requestActiveAuctions()
    }
}
