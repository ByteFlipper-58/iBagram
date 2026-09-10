package org.telegram.messenger.feature.business.stargifts.presentation

import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter

sealed interface StarGiftsEvent {
    data class LoadCatalog(val forceRefresh: Boolean = false) : StarGiftsEvent
    data class LoadProfileGifts(val dialogId: Long, val offset: String? = null) : StarGiftsEvent
    data class SelectGift(val giftId: Long) : StarGiftsEvent
    data class TogglePin(val dialogId: Long, val giftId: Long, val pin: Boolean) : StarGiftsEvent
    data class ToggleHide(val dialogId: Long, val giftId: Long, val hide: Boolean) : StarGiftsEvent
    data class ApplyFilter(val dialogId: Long, val filter: StarGiftFilter) : StarGiftsEvent
    object ClearError : StarGiftsEvent
}
