package org.telegram.messenger.feature.hints.domain.model

/**
 * Pure domain enum representing all user prompts, tips, and feature discovery hints.
 * Decoupled from Android SharedPreferences and UI views.
 */
enum class HintType(
    val preferenceKey: String,
    val showsLimit: Int,
    val probability: Float
) {
    ROUND_HINT_2("needShowRoundHint2", 3, 0.2f),
    ROUND_HINT_CHANNEL_2("needShowRoundHintChannel2", 3, 0.2f),
    CHANNEL_SUGGEST_HINT("channelsuggesthint", 3, 0.2f),
    CHANNEL_GIFT_HINT("channelgifthint", 3, 0.2f),
    GROUP_EMOJI_PACK_HINT_SHOWN("groupEmojiPackShownHint", 1, 1.0f),
    ACCOUNT_SWITCH_HINT("accountswitchhint", 3, 1.0f),
    GIFT_MESSAGE_HINT("giftMessaheHint", 3, 1.0f),
    GUEST_BOT_PRIVACY("hints_controller_GuestBotPrivacy", 3, 1.0f);

    companion object {
        fun fromKey(key: String): HintType? {
            return entries.firstOrNull { it.preferenceKey == key }
        }
    }
}
