package org.telegram.messenger.feature.boosts.data.mapper

import org.telegram.messenger.ChannelBoostsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.feature.boosts.domain.model.BoostSlotModel
import org.telegram.messenger.feature.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.boosts.domain.model.MyBoostsModel
import org.telegram.tgnet.tl.TL_stories

object BoostMapper {

    fun mapToBoostStatus(status: TL_stories.TL_premium_boostsStatus): BoostStatusModel {
        val nextLvl = status.next_level_boosts
        return BoostStatusModel(
            level = status.level,
            currentLevelBoosts = status.current_level_boosts,
            boosts = status.boosts,
            giftBoosts = status.gift_boosts,
            nextLevelBoosts = nextLvl,
            premiumAudiencePart = status.premium_audience?.part ?: 0.0,
            premiumAudienceTotal = status.premium_audience?.total ?: 0.0,
            boostUrl = status.boost_url ?: "",
            hasMyBoost = status.my_boost,
            myBoostSlots = status.my_boost_slots?.toList() ?: emptyList(),
            isMaxLevel = nextLvl <= 0
        )
    }

    fun mapToBoostSlot(boost: TL_stories.TL_myBoost): BoostSlotModel {
        val peerId = boost.peer?.let { DialogObject.getPeerDialogId(it) }
        return BoostSlotModel(
            slot = boost.slot,
            peerDialogId = peerId,
            date = boost.date,
            expires = boost.expires,
            cooldownUntilDate = boost.cooldown_until_date
        )
    }

    fun mapToMyBoosts(myBoosts: TL_stories.TL_premium_myBoosts): MyBoostsModel {
        val list = myBoosts.my_boosts?.map { mapToBoostSlot(it) } ?: emptyList()
        return MyBoostsModel(slots = list)
    }

    fun mapToCanApplyBoost(canApply: ChannelBoostsController.CanApplyBoost): CanApplyBoostModel {
        return CanApplyBoostModel(
            canApply = canApply.canApply,
            empty = canApply.empty,
            replaceDialogId = canApply.replaceDialogId,
            alreadyActive = canApply.alreadyActive,
            needSelector = canApply.needSelector,
            floodWait = canApply.floodWait,
            slot = canApply.slot,
            boostCount = canApply.boostCount,
            isMaxLevel = canApply.isMaxLvl
        )
    }
}
