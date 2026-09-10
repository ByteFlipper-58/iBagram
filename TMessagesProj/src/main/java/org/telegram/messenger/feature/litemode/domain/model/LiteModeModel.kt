package org.telegram.messenger.feature.litemode.domain.model

enum class LiteModeFlag(val bitMask: Int) {
    ANIMATED_STICKERS_KEYBOARD(1),
    ANIMATED_STICKERS_CHAT(2),
    ANIMATED_EMOJI_KEYBOARD_PREMIUM(4),
    ANIMATED_EMOJI_REACTIONS_PREMIUM(8),
    ANIMATED_EMOJI_CHAT_PREMIUM(16),
    CHAT_BACKGROUND(32),
    CHAT_FORUM_TWOCOLUMN(64),
    CHAT_SPOILER(128),
    CHAT_BLUR(256),
    CALLS_ANIMATIONS(512),
    AUTOPLAY_VIDEOS(1024),
    AUTOPLAY_GIFS(2048),
    ANIMATED_EMOJI_CHAT_NOT_PREMIUM(4096),
    ANIMATED_EMOJI_REACTIONS_NOT_PREMIUM(8192),
    ANIMATED_EMOJI_KEYBOARD_NOT_PREMIUM(16384),
    CHAT_SCALE(32768),
    CHAT_THANOS(65536),
    PARTICLES(131072),
    LIQUID_GLASS(262144);

    companion object {
        const val FLAGS_ANIMATED_STICKERS = 1 or 2
        const val FLAGS_ANIMATED_EMOJI = 4 or 16384 or 8 or 8192 or 16 or 4096
        const val FLAGS_CHAT = 32 or 64 or 128 or 256 or 32768 or 65536 or 262144

        fun fromMask(mask: Int): Set<LiteModeFlag> {
            return values().filter { (mask and it.bitMask) != 0 }.toSet()
        }
    }
}

enum class LiteModePreset(val value: Int) {
    POWER_SAVER(0),
    LOW(198684),
    MEDIUM(204383),
    HIGH(262143),
    CUSTOM(-1);

    companion object {
        fun fromValue(value: Int): LiteModePreset {
            return values().firstOrNull { it != CUSTOM && it.value == value } ?: CUSTOM
        }
    }
}

data class LiteModeState(
    val rawFlags: Int = LiteModePreset.HIGH.value,
    val powerSaverThreshold: Int = 10,
    val batteryLevel: Int = 100,
    val isPowerSaverActive: Boolean = false,
    val isTablet: Boolean = false,
    val hasPremium: Boolean = false
) {
    val preset: LiteModePreset get() = LiteModePreset.fromValue(rawFlags)
    val effectiveFlags: Int
        get() {
            if (isPowerSaverActive) return LiteModePreset.POWER_SAVER.value
            var flags = rawFlags
            // Process premium flags
            if ((flags and (4 or 16384)) > 0) {
                flags = (flags and (4 or 16384).inv()) or if (hasPremium) 4 else 16384
            }
            if ((flags and (8 or 8192)) > 0) {
                flags = (flags and (8 or 8192).inv()) or if (hasPremium) 8 else 8192
            }
            if ((flags and (16 or 4096)) > 0) {
                flags = (flags and (16 or 4096).inv()) or if (hasPremium) 16 else 4096
            }
            if (isTablet) {
                flags = flags or LiteModeFlag.CHAT_FORUM_TWOCOLUMN.bitMask
            }
            return flags
        }

    fun isEnabled(flag: LiteModeFlag): Boolean {
        if (flag == LiteModeFlag.CHAT_FORUM_TWOCOLUMN && isTablet) return true
        return (effectiveFlags and flag.bitMask) != 0
    }

    fun isEnabledSetting(flag: LiteModeFlag): Boolean {
        return (rawFlags and flag.bitMask) != 0
    }
}
