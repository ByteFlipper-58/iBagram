package org.telegram.messenger.feature.business.stargifts.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local data source managing cached Star Gifts catalog and profile gifts.
 */
open class StarGiftsLocalDataSource(
    private val currentAccount: Int
) {
    private var isTestMode = false

    private val testCatalog = CopyOnWriteArrayList<TL_stars.StarGift>()
    private val testGifts = ConcurrentHashMap<Long, TL_stars.StarGift>()
    private val testProfileGifts = ConcurrentHashMap<Long, MutableList<TL_stars.SavedStarGift>>()

    private val testCatalogFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val testProfileGiftsFlow = MutableSharedFlow<Long>(extraBufferCapacity = 16)

    fun setTestMode(testMode: Boolean) {
        this.isTestMode = testMode
    }

    fun setTestCatalog(gifts: List<TL_stars.StarGift>) {
        testCatalog.clear()
        testCatalog.addAll(gifts)
        for (g in gifts) {
            testGifts[g.id] = g
        }
        testCatalogFlow.tryEmit(Unit)
    }

    fun setTestGift(gift: TL_stars.StarGift) {
        testGifts[gift.id] = gift
    }

    fun setTestProfileGifts(dialogId: Long, gifts: List<TL_stars.SavedStarGift>) {
        testProfileGifts[dialogId] = CopyOnWriteArrayList(gifts)
        testProfileGiftsFlow.tryEmit(dialogId)
    }

    open fun getController(): StarsController? {
        if (isTestMode) return null
        return try {
            StarsController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun observeCatalogUpdated(): Flow<Unit> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.starGiftsLoaded)
                .map { Unit }
        } catch (_: Throwable) {
            testCatalogFlow.asSharedFlow()
        }
    }

    open fun observeProfileGiftsUpdated(): Flow<Long> {
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.starUserGiftsLoaded)
                .map { event -> (if (event.args.isNotEmpty()) event.args[0] as? Long else null) ?: 0L }
        } catch (_: Throwable) {
            testProfileGiftsFlow.asSharedFlow()
        }
    }

    open fun getCatalog(): List<TL_stars.StarGift> {
        if (testCatalog.isNotEmpty()) return testCatalog.toList()
        val controller = getController() ?: return emptyList()
        return try {
            controller.sortedGifts ?: controller.gifts ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveCatalog(gifts: List<TL_stars.StarGift>) {
        testCatalog.clear()
        testCatalog.addAll(gifts)
        for (g in gifts) {
            testGifts[g.id] = g
        }
    }

    open fun getGift(giftId: Long): TL_stars.StarGift? {
        testGifts[giftId]?.let { return it }
        val controller = getController() ?: return null
        return try {
            controller.getStarGift(giftId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun saveGift(gift: TL_stars.StarGift) {
        testGifts[gift.id] = gift
    }

    open fun getProfileGifts(dialogId: Long): List<TL_stars.SavedStarGift> {
        testProfileGifts[dialogId]?.let { return it.toList() }
        val controller = getController() ?: return emptyList()
        return try {
            controller.getProfileGiftsList(dialogId)?.gifts ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun saveProfileGifts(dialogId: Long, gifts: List<TL_stars.SavedStarGift>) {
        testProfileGifts[dialogId] = CopyOnWriteArrayList(gifts)
    }

    open fun togglePin(dialogId: Long, giftId: Long, pin: Boolean): Boolean {
        val list = testProfileGifts[dialogId]
        if (list != null) {
            val gift = list.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }
            if (gift != null) {
                gift.pinned_to_top = pin
                return true
            }
        }
        val controller = getController() ?: return false
        return try {
            val profileList = controller.getProfileGiftsList(dialogId) ?: return false
            val gift = profileList.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId } ?: return false
            val hitLimit = profileList.togglePinned(gift, pin, false)
            !hitLimit
        } catch (_: Throwable) {
            false
        }
    }

    open fun toggleHide(dialogId: Long, giftId: Long, hide: Boolean): Boolean {
        val list = testProfileGifts[dialogId]
        if (list != null) {
            val gift = list.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId }
            if (gift != null) {
                gift.unsaved = hide
                return true
            }
        }
        val controller = getController() ?: return false
        return try {
            val profileList = controller.getProfileGiftsList(dialogId) ?: return false
            val gift = profileList.gifts?.firstOrNull { (if (it.saved_id != 0L) it.saved_id else it.msg_id.toLong()) == giftId } ?: return false
            gift.unsaved = hide
            profileList.updateGiftsUnsaved(gift, hide)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
