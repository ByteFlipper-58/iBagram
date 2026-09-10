package org.telegram.messenger.feature.business.stargifts.data.mapper

import org.telegram.messenger.DialogObject
import org.telegram.messenger.feature.business.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.business.stargifts.domain.model.SavedStarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftsCatalogModel
import org.telegram.tgnet.tl.TL_stars

object StarGiftMapper {

    fun mapStarGift(gift: TL_stars.StarGift?): StarGiftModel? {
        if (gift == null) return null
        return StarGiftModel(
            id = gift.id,
            stars = gift.stars,
            slug = gift.slug,
            title = gift.title,
            stickerDocumentId = gift.sticker?.id,
            availabilityRemains = if (gift.limited) gift.availability_remains else null,
            availabilityTotal = if (gift.limited) gift.availability_total else null,
            isSoldOut = gift.sold_out,
            isBirthday = gift.birthday,
            isLimited = gift.limited,
            upgradeStars = if (gift.upgrade_stars > 0) gift.upgrade_stars else null,
            transferStars = null,
            canExportAt = null,
        )
    }

    fun mapSavedStarGift(saved: TL_stars.SavedStarGift?): SavedStarGiftModel? {
        if (saved == null || saved.gift == null) return null
        val giftModel = mapStarGift(saved.gift) ?: return null
        val fromId = if (saved.from_id != null) DialogObject.getPeerDialogId(saved.from_id) else null
        val id = if (saved.saved_id != 0L) saved.saved_id else saved.msg_id.toLong()

        return SavedStarGiftModel(
            id = id,
            date = saved.date,
            gift = giftModel,
            message = saved.message?.text,
            fromPeerId = fromId,
            isPinnedToTop = saved.pinned_to_top,
            isUnsaved = saved.unsaved,
            canUpgrade = saved.can_upgrade,
            convertStars = if (saved.convert_stars > 0) saved.convert_stars else null,
            upgradeStars = if (saved.upgrade_stars > 0) saved.upgrade_stars else null,
            transferStars = if (saved.transfer_stars > 0) saved.transfer_stars else null,
        )
    }

    fun mapStarGiftList(gifts: List<TL_stars.StarGift>?): List<StarGiftModel> {
        if (gifts == null) return emptyList()
        return gifts.mapNotNull { mapStarGift(it) }
    }

    fun mapSavedStarGiftList(savedGifts: List<TL_stars.SavedStarGift>?): List<SavedStarGiftModel> {
        if (savedGifts == null) return emptyList()
        return savedGifts.mapNotNull { mapSavedStarGift(it) }
    }

    fun mapCatalog(gifts: List<TL_stars.StarGift>?, isLoading: Boolean = false): StarGiftsCatalogModel {
        return StarGiftsCatalogModel(
            gifts = mapStarGiftList(gifts),
            isLoading = isLoading
        )
    }

    fun mapProfileGifts(
        dialogId: Long,
        savedGifts: List<TL_stars.SavedStarGift>?,
        totalCount: Int = 0,
        hasMore: Boolean = false,
        nextOffset: String? = null,
        isLoading: Boolean = false
    ): ProfileGiftsModel {
        return ProfileGiftsModel(
            dialogId = dialogId,
            gifts = mapSavedStarGiftList(savedGifts),
            totalCount = totalCount,
            hasMore = hasMore,
            nextOffset = nextOffset,
            isLoading = isLoading
        )
    }
}
