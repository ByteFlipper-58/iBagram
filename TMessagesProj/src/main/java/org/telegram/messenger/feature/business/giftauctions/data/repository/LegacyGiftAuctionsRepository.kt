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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.data.mapper.GiftAuctionMapper
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository
import kotlin.coroutines.resume

class LegacyGiftAuctionsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : GiftAuctionsRepository {

    override fun observeActiveAuctions(): Flow<List<GiftAuctionModel>> = callbackFlow {
        val controller = GiftAuctionController.getInstance(currentAccount)
        val listener = GiftAuctionController.OnActiveAuctionsUpdateListeners { auctions ->
            val models = auctions?.mapNotNull { GiftAuctionMapper.mapAuction(it) } ?: emptyList()
            trySend(models)
        }

        controller.subscribeToActiveAuctionsUpdates(listener)
        val current = controller.activeAuctions?.mapNotNull { GiftAuctionMapper.mapAuction(it) } ?: emptyList()
        trySend(current)

        awaitClose {
            controller.unsubscribeFromActiveAuctionsUpdates(listener)
        }
    }.flowOn(mainDispatcher)

    override fun observeAuction(giftId: Long): Flow<GiftAuctionModel?> = callbackFlow {
        val controller = GiftAuctionController.getInstance(currentAccount)
        val listener = GiftAuctionController.OnAuctionUpdateListener { auction ->
            trySend(GiftAuctionMapper.mapAuction(auction))
        }

        val initial = controller.subscribeToGiftAuction(giftId, listener)
        if (initial != null) {
            trySend(GiftAuctionMapper.mapAuction(initial))
        }

        awaitClose {
            controller.unsubscribeFromGiftAuction(giftId, listener)
        }
    }.flowOn(mainDispatcher)

    override suspend fun getActiveAuctions(): List<GiftAuctionModel> = withContext(mainDispatcher) {
        val controller = GiftAuctionController.getInstance(currentAccount)
        controller.activeAuctions?.mapNotNull { GiftAuctionMapper.mapAuction(it) } ?: emptyList()
    }

    override suspend fun getAuctionById(giftId: Long): Result<GiftAuctionModel> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = GiftAuctionController.getInstance(currentAccount)
            controller.getOrRequestAuction(giftId) { auction, error ->
                if (auction != null) {
                    val model = GiftAuctionMapper.mapAuction(auction)
                    if (model != null) {
                        cont.resume(Result.Success(model))
                    } else {
                        cont.resume(Result.failure("Failed to map auction"))
                    }
                } else {
                    cont.resume(Result.failure(error?.text ?: "Failed to get auction"))
                }
            }
        }
    }

    override suspend fun getAuctionBySlug(slug: String): Result<GiftAuctionModel> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = GiftAuctionController.getInstance(currentAccount)
            controller.requestGiftAuctionBySlug(slug) { res, error ->
                if (res != null) {
                    val auction = controller.getAuction(res.gift.id)
                    val model = GiftAuctionMapper.mapAuction(auction)
                    if (model != null) {
                        cont.resume(Result.Success(model))
                    } else {
                        cont.resume(Result.failure("Failed to map auction from slug"))
                    }
                } else {
                    cont.resume(Result.failure(error?.text ?: "Failed to get auction by slug"))
                }
            }
        }
    }

    override suspend fun sendBid(
        giftId: Long,
        amount: Long,
        params: GiftAuctionBidParamsModel?
    ): Result<Unit> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = GiftAuctionController.getInstance(currentAccount)
            val legacyParams = GiftAuctionMapper.toLegacyBidParams(params)
            controller.sendBid(giftId, legacyParams, amount) { success, error ->
                if (success) {
                    cont.resume(Result.Success(Unit))
                } else {
                    cont.resume(Result.failure(error ?: "Failed to send bid"))
                }
            }
        }
    }

    override suspend fun loadAcquiredGifts(giftId: Long): Result<List<GiftAuctionAcquiredGiftModel>> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = GiftAuctionController.getInstance(currentAccount)
            controller.getOrRequestAcquiredGifts(giftId) { gifts ->
                if (gifts != null) {
                    val models = gifts.mapNotNull { GiftAuctionMapper.mapAcquiredGift(giftId, it) }
                    cont.resume(Result.Success(models))
                } else {
                    cont.resume(Result.failure("Failed to load acquired gifts"))
                }
            }
        }
    }

    override suspend fun refreshActiveAuctions(): Result<Unit> = withContext(mainDispatcher) {
        try {
            GiftAuctionController.getInstance(currentAccount).requestUserAuctions()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to refresh auctions", e)
        }
    }
}
