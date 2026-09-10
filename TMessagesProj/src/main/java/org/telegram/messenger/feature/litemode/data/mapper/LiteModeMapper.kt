package org.telegram.messenger.feature.litemode.data.mapper

import org.telegram.messenger.LiteMode
import org.telegram.messenger.feature.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.litemode.domain.model.LiteModePreset

object LiteModeMapper {

    fun mapFlagToLegacy(flag: LiteModeFlag): Int {
        return when (flag) {
            LiteModeFlag.ANIMATED_STICKERS_KEYBOARD -> LiteMode.FLAG_ANIMATED_STICKERS_KEYBOARD
            LiteModeFlag.ANIMATED_STICKERS_CHAT -> LiteMode.FLAG_ANIMATED_STICKERS_CHAT
            LiteModeFlag.ANIMATED_EMOJI_KEYBOARD_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_KEYBOARD_PREMIUM
            LiteModeFlag.ANIMATED_EMOJI_REACTIONS_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_REACTIONS_PREMIUM
            LiteModeFlag.ANIMATED_EMOJI_CHAT_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_CHAT_PREMIUM
            LiteModeFlag.CHAT_BACKGROUND -> LiteMode.FLAG_CHAT_BACKGROUND
            LiteModeFlag.CHAT_FORUM_TWOCOLUMN -> LiteMode.FLAG_CHAT_FORUM_TWOCOLUMN
            LiteModeFlag.CHAT_SPOILER -> LiteMode.FLAG_CHAT_SPOILER
            LiteModeFlag.CHAT_BLUR -> LiteMode.FLAG_CHAT_BLUR
            LiteModeFlag.CALLS_ANIMATIONS -> LiteMode.FLAG_CALLS_ANIMATIONS
            LiteModeFlag.AUTOPLAY_VIDEOS -> LiteMode.FLAG_AUTOPLAY_VIDEOS
            LiteModeFlag.AUTOPLAY_GIFS -> LiteMode.FLAG_AUTOPLAY_GIFS
            LiteModeFlag.ANIMATED_EMOJI_CHAT_NOT_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_CHAT_NOT_PREMIUM
            LiteModeFlag.ANIMATED_EMOJI_REACTIONS_NOT_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_REACTIONS_NOT_PREMIUM
            LiteModeFlag.ANIMATED_EMOJI_KEYBOARD_NOT_PREMIUM -> LiteMode.FLAG_ANIMATED_EMOJI_KEYBOARD_NOT_PREMIUM
            LiteModeFlag.CHAT_SCALE -> LiteMode.FLAG_CHAT_SCALE
            LiteModeFlag.CHAT_THANOS -> LiteMode.FLAG_CHAT_THANOS
            LiteModeFlag.PARTICLES -> LiteMode.FLAG_PARTICLES
            LiteModeFlag.LIQUID_GLASS -> LiteMode.FLAG_LIQUID_GLASS
        }
    }

    fun mapPresetToLegacy(preset: LiteModePreset): Int {
        return when (preset) {
            LiteModePreset.POWER_SAVER -> LiteMode.PRESET_POWER_SAVER
            LiteModePreset.LOW -> LiteMode.PRESET_LOW
            LiteModePreset.MEDIUM -> LiteMode.PRESET_MEDIUM
            LiteModePreset.HIGH -> LiteMode.PRESET_HIGH
            LiteModePreset.CUSTOM -> -1
        }
    }

    fun mapLegacyToPreset(value: Int): LiteModePreset {
        return when (value) {
            LiteMode.PRESET_POWER_SAVER -> LiteModePreset.POWER_SAVER
            LiteMode.PRESET_LOW -> LiteModePreset.LOW
            LiteMode.PRESET_MEDIUM -> LiteModePreset.MEDIUM
            LiteMode.PRESET_HIGH -> LiteModePreset.HIGH
            else -> LiteModePreset.CUSTOM
        }
    }
}
