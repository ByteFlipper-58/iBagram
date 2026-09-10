package org.telegram.messenger.feature.system.hints.data.mapper

import org.telegram.messenger.feature.system.hints.domain.model.HintModel
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.ui.Components.HintsController

/**
 * Mapper between legacy HintsController.Hint and pure domain models.
 */
object HintMapper {

    fun toDomainType(legacy: HintsController.Hint): HintType {
        return when (legacy) {
            HintsController.Hint.RoundHint2 -> HintType.ROUND_HINT_2
            HintsController.Hint.RoundHintChannel2 -> HintType.ROUND_HINT_CHANNEL_2
            HintsController.Hint.ChannelSuggestHint -> HintType.CHANNEL_SUGGEST_HINT
            HintsController.Hint.ChannelGiftHint -> HintType.CHANNEL_GIFT_HINT
            HintsController.Hint.GroupEmojiPackHintShown -> HintType.GROUP_EMOJI_PACK_HINT_SHOWN
            HintsController.Hint.AccountSwitchHint -> HintType.ACCOUNT_SWITCH_HINT
            HintsController.Hint.GiftMessageHint -> HintType.GIFT_MESSAGE_HINT
            HintsController.Hint.GuestBotPrivacy -> HintType.GUEST_BOT_PRIVACY
        }
    }

    fun toLegacyHint(domain: HintType): HintsController.Hint {
        return when (domain) {
            HintType.ROUND_HINT_2 -> HintsController.Hint.RoundHint2
            HintType.ROUND_HINT_CHANNEL_2 -> HintsController.Hint.RoundHintChannel2
            HintType.CHANNEL_SUGGEST_HINT -> HintsController.Hint.ChannelSuggestHint
            HintType.CHANNEL_GIFT_HINT -> HintsController.Hint.ChannelGiftHint
            HintType.GROUP_EMOJI_PACK_HINT_SHOWN -> HintsController.Hint.GroupEmojiPackHintShown
            HintType.ACCOUNT_SWITCH_HINT -> HintsController.Hint.AccountSwitchHint
            HintType.GIFT_MESSAGE_HINT -> HintsController.Hint.GiftMessageHint
            HintType.GUEST_BOT_PRIVACY -> HintsController.Hint.GuestBotPrivacy
        }
    }

    fun toDomainModel(type: HintType, showsCount: Int): HintModel {
        return HintModel(
            type = type,
            showsCount = showsCount,
            showsLimit = type.showsLimit,
            probability = type.probability
        )
    }
}
