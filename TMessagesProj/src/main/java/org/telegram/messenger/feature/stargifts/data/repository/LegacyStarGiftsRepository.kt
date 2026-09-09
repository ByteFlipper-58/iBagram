package org.telegram.messenger.feature.stargifts.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.data.mapper.StarGiftMapper
import org.telegram.messenger.feature.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftsCatalogModel
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController
import kotlin.coroutines.resume

class LegacyStarGiftsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : StarGiftsRepository {

    private val starsController: StarsController
        get() = StarsController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override fun observeCatalog(): Flow<StarGiftsCatalogModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.starGiftsLoaded
        )
            .map {
                val gifts = starsController.sortedGifts ?: starsController.gifts
                StarGiftMapper.mapCatalog(gifts, isLoading = false)
            }
            .onStart {
                starsController.loadStarGifts()
                val gifts = starsController.sortedGifts ?: starsController.gifts
                emit(StarGiftMapper.mapCatalog(gifts, isLoading = gifts == null || gifts.isEmpty()))
            }
            .flowOn(mainDispatcher)
    }

    override suspend fun getCatalog(forceRefresh: Boolean): Result<List<StarGiftModel>> =
        withContext(mainDispatcher) {
            val currentGifts = starsController.sortedGifts ?: starsController.gifts
            if (!forceRefresh && currentGifts != null && currentGifts.isNotEmpty()) {
                return@withContext Result.Success(StarGiftMapper.mapStarGiftList(currentGifts))
            }

            suspendCancellableCoroutine<Result<List<StarGiftModel>>> { continuation ->
                val cancelable = starsController.getStarGift(0L) {
                    if (continuation.isActive) {
                        val gifts = starsController.sortedGifts ?: starsController.gifts
                        continuation.resume(Result.Success(StarGiftMapper.mapStarGiftList(gifts)))
                    }
                }
                starsController.loadStarGifts()
                continuation.invokeOnCancellation {
                    cancelable?.run()
                }
            }
        }

    override suspend fun getGift(giftId: Long): Result<StarGiftModel?> =
        withContext(mainDispatcher) {
            val localGift = starsController.getStarGift(giftId)
            if (localGift != null) {
                return@withContext Result.Success(StarGiftMapper.mapStarGift(localGift))
            }

            suspendCancellableCoroutine<Result<StarGiftModel?>> { continuation ->
                val cancelable = starsController.getStarGift(giftId) { gift ->
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(StarGiftMapper.mapStarGift(gift)))
                    }
                }
                continuation.invokeOnCancellation {
                    cancelable?.run()
                }
            }
        }

    override fun observeProfileGifts(dialogId: Long): Flow<ProfileGiftsModel> {
        return NotificationCenterFlowBridge.observeEvent(
            currentAccount,
            NotificationCenter.starUserGiftsLoaded
        )
            .map {
                val list = starsController.getProfileGiftsList(dialogId)
                if (list != null) {
                    StarGiftMapper.mapProfileGifts(
                        dialogId = dialogId,
                        savedGifts = list.gifts,
                        totalCount = list.totalCount,
                        hasMore = !list.endReached,
                        nextOffset = list.lastOffset,
                        isLoading = list.loading
                    )
                } else {
                    ProfileGiftsModel(dialogId = dialogId)
                }
            }
            .onStart {
                val list = starsController.getProfileGiftsList(dialogId)
                if (list != null) {
                    emit(
                        StarGiftMapper.mapProfileGifts(
                            dialogId = dialogId,
                            savedGifts = list.gifts,
                            totalCount = list.totalCount,
                            hasMore = !list.endReached,
                            nextOffset = list.lastOffset,
                            isLoading = list.loading
                        )
                    )
                } else {
                    emit(ProfileGiftsModel(dialogId = dialogId, isLoading = true))
                }
            }
            .flowOn(mainDispatcher)
    }

    override suspend fun loadProfileGifts(
        dialogId: Long,
        offset: String?,
        limit: Int,
        filter: StarGiftFilter
    ): Result<ProfileGiftsModel> =
        withContext(mainDispatcher) {
            suspendCancellableCoroutine<Result<ProfileGiftsModel>> { continuation ->
                val req = TL_stars.getSavedStarGifts()
                req.sort_by_value = !filter.sortByDate
                req.exclude_unlimited = !filter.includeUnlimited
                req.exclude_unupgradable = !filter.includeLimited
                req.exclude_upgradable = !filter.includeUpgradable
                req.exclude_unique = !filter.includeUnique
                req.exclude_saved = !filter.includeDisplayed
                req.exclude_unsaved = !filter.includeHidden
                req.peer = if (dialogId == 0L) TLRPC.TL_inputPeerSelf() else messagesController.getInputPeer(dialogId)
                req.offset = offset ?: ""
                req.limit = limit

                val reqId = connectionsManager.sendRequest(req) { response, error ->
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resume(Result.failure("Failed to load saved star gifts: ${error.text}"))
                        } else if (response is TL_stars.TL_payments_savedStarGifts) {
                            messagesController.putUsers(response.users, false)
                            messagesController.putChats(response.chats, false)

                            val hasMore = response.gifts.size >= limit && response.next_offset != null
                            val model = StarGiftMapper.mapProfileGifts(
                                dialogId = dialogId,
                                savedGifts = response.gifts,
                                totalCount = response.count,
                                hasMore = hasMore,
                                nextOffset = response.next_offset,
                                isLoading = false
                            )
                            continuation.resume(Result.Success(model))
                        } else {
                            continuation.resume(Result.failure("Unexpected response loading star gifts"))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(reqId, true)
                }
            }
        }

    override suspend fun togglePinGift(dialogId: Long, giftId: Long, pin: Boolean): Result<Boolean> =
        withContext(mainDispatcher) {
            val list = starsController.getProfileGiftsList(dialogId)
            val gift = list?.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }

            if (list != null && gift != null) {
                val hitLimit = list.togglePinned(gift, pin, false)
                Result.Success(!hitLimit)
            } else {
                Result.failure("Gift $giftId not found in profile gifts")
            }
        }

    override suspend fun toggleHideGift(dialogId: Long, giftId: Long, hide: Boolean): Result<Boolean> =
        withContext(mainDispatcher) {
            val list = starsController.getProfileGiftsList(dialogId)
            val gift = list?.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }

            if (gift == null) {
                return@withContext Result.failure("Gift $giftId not found in profile gifts")
            }

            val input = list.getInput(gift)
                ?: return@withContext Result.failure("Cannot build input for gift $giftId")

            suspendCancellableCoroutine<Result<Boolean>> { continuation ->
                val req = TL_stars.saveStarGift()
                req.stargift = input
                req.unsave = hide

                val reqId = connectionsManager.sendRequest(req) { response, error ->
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resume(Result.failure("Failed to toggle hide gift: ${error.text}"))
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
            }
        }
}
