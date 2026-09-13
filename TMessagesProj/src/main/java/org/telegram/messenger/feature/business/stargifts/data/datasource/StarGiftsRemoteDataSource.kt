package org.telegram.messenger.feature.business.stargifts.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController
import kotlin.coroutines.resume

/**
 * Remote data source for fetching Star Gifts catalog and managing saved/profile gifts via MTProto.
 */
open class StarGiftsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchCatalog(forceRefresh: Boolean = false): Result<List<TL_stars.StarGift>> =
        suspendCancellableCoroutine { continuation ->
            try {
                val starsController = StarsController.getInstance(currentAccount)
                val cancelable = starsController.getStarGift(0L) {
                    if (continuation.isActive) {
                        val gifts = starsController.sortedGifts ?: starsController.gifts ?: emptyList()
                        continuation.resume(Result.Success(gifts))
                    }
                }
                starsController.loadStarGifts()
                continuation.invokeOnCancellation {
                    cancelable?.run()
                }
            } catch (e: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to load star gifts catalog", e)))
                }
            }
        }

    open suspend fun fetchGift(giftId: Long): Result<TL_stars.StarGift?> =
        suspendCancellableCoroutine { continuation ->
            try {
                val starsController = StarsController.getInstance(currentAccount)
                val localGift = starsController.getStarGift(giftId)
                if (localGift != null) {
                    continuation.resume(Result.Success(localGift))
                    return@suspendCancellableCoroutine
                }

                val cancelable = starsController.getStarGift(giftId) { gift ->
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(gift))
                    }
                }
                continuation.invokeOnCancellation {
                    cancelable?.run()
                }
            } catch (e: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to load star gift $giftId", e)))
                }
            }
        }

    open suspend fun fetchProfileGifts(
        dialogId: Long,
        offset: String? = null,
        limit: Int = 30,
        filter: StarGiftFilter = StarGiftFilter()
    ): Result<TL_stars.TL_payments_savedStarGifts> =
        suspendCancellableCoroutine { continuation ->
            try {
                val req = TL_stars.getSavedStarGifts()
                req.sort_by_value = !filter.sortByDate
                req.exclude_unlimited = !filter.includeUnlimited
                req.exclude_unupgradable = !filter.includeLimited
                req.exclude_upgradable = !filter.includeUpgradable
                req.exclude_unique = !filter.includeUnique
                req.exclude_saved = !filter.includeDisplayed
                req.exclude_unsaved = !filter.includeHidden
                val messagesController = MessagesController.getInstance(currentAccount)
                req.peer = if (dialogId == 0L) TLRPC.TL_inputPeerSelf() else messagesController.getInputPeer(dialogId)
                req.offset = offset ?: ""
                req.limit = limit

                val reqId = connectionsManager.sendRequest(req) { response, error ->
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resume(Result.Failure(AppError.Network(error.text ?: "Failed to load saved star gifts", error.code)))
                        } else if (response is TL_stars.TL_payments_savedStarGifts) {
                            try {
                                messagesController.putUsers(response.users, false)
                                messagesController.putChats(response.chats, false)
                            } catch (_: Throwable) {}
                            continuation.resume(Result.Success(response))
                        } else {
                            continuation.resume(Result.Failure(AppError.Generic("Unexpected response loading star gifts")))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(reqId, true)
                }
            } catch (e: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to request saved star gifts", e)))
                }
            }
        }

    open suspend fun togglePinGift(dialogId: Long, giftId: Long, pin: Boolean): Result<Boolean> {
        return try {
            val starsController = StarsController.getInstance(currentAccount)
            val list = starsController.getProfileGiftsList(dialogId)
            val gift = list?.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }

            if (list != null && gift != null) {
                val hitLimit = list.togglePinned(gift, pin, false)
                Result.Success(!hitLimit)
            } else {
                Result.Failure(AppError.Generic("Gift $giftId not found in profile gifts"))
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to toggle pin gift", e))
        }
    }

    open suspend fun toggleHideGift(dialogId: Long, giftId: Long, hide: Boolean): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            try {
                val starsController = StarsController.getInstance(currentAccount)
                val list = starsController.getProfileGiftsList(dialogId)
                val gift = list?.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }

                if (gift == null) {
                    continuation.resume(Result.Failure(AppError.Generic("Gift $giftId not found in profile gifts")))
                    return@suspendCancellableCoroutine
                }

                val input = list?.getInput(gift)
                if (input == null) {
                    continuation.resume(Result.Failure(AppError.Generic("Cannot build input for gift $giftId")))
                    return@suspendCancellableCoroutine
                }

                val req = TL_stars.saveStarGift()
                req.stargift = input
                req.unsave = hide

                val reqId = connectionsManager.sendRequest(req) { _, error ->
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resume(Result.Failure(AppError.Network(error.text ?: "Failed to toggle hide gift", error.code)))
                        } else {
                            gift.unsaved = hide
                            list.updateGiftsUnsaved(gift, hide)
                            continuation.resume(Result.Success(true))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(reqId, true)
                }
            } catch (e: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(Result.Failure(AppError.Generic(e.message ?: "Failed to toggle hide gift", e)))
                }
            }
        }
}
