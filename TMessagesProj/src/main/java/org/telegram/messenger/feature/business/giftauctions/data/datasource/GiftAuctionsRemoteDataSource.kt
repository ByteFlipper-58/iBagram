package org.telegram.messenger.feature.business.giftauctions.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.GiftAuctionController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars
import kotlin.coroutines.resume

/**
 * Remote data source for MTProto Star Gift auction RPCs.
 */
open class GiftAuctionsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getAuctionById(giftId: Long): Result<TL_payments.TL_StarGiftAuctionState> =
        suspendCancellableCoroutine { cont ->
            try {
                val controller = GiftAuctionController.getInstance(currentAccount)
                controller.requestGiftAuctionById(giftId) { res, err ->
                    if (res != null) {
                        cont.resume(Result.Success(res))
                    } else {
                        cont.resume(Result.Failure(AppError.Network(err?.text ?: "Failed to get auction state", err?.code ?: -1)))
                    }
                }
            } catch (e: Throwable) {
                cont.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to request auction", e)))
            }
        }

    open suspend fun getAuctionBySlug(slug: String): Result<TL_payments.TL_StarGiftAuctionState> =
        suspendCancellableCoroutine { cont ->
            try {
                val controller = GiftAuctionController.getInstance(currentAccount)
                controller.requestGiftAuctionBySlug(slug) { res, err ->
                    if (res != null) {
                        cont.resume(Result.Success(res))
                    } else {
                        cont.resume(Result.Failure(AppError.Network(err?.text ?: "Failed to get auction state by slug", err?.code ?: -1)))
                    }
                }
            } catch (e: Throwable) {
                cont.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to request auction by slug", e)))
            }
        }

    open suspend fun loadAcquiredGifts(giftId: Long): Result<List<TL_stars.TL_StarGiftAuctionAcquiredGift>> =
        suspendCancellableCoroutine { cont ->
            try {
                val controller = GiftAuctionController.getInstance(currentAccount)
                controller.getOrRequestAcquiredGifts(giftId) { gifts ->
                    if (gifts != null) {
                        cont.resume(Result.Success(gifts))
                    } else {
                        cont.resume(Result.Failure(AppError.Network("Failed to load acquired gifts", -1)))
                    }
                }
            } catch (e: Throwable) {
                cont.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to load acquired gifts", e)))
            }
        }

    open suspend fun requestActiveAuctions(): Result<Unit> {
        return try {
            GiftAuctionController.getInstance(currentAccount).requestUserAuctions()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to request active auctions", e))
        }
    }
}
