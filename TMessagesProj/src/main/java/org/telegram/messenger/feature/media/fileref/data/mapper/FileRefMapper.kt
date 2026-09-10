package org.telegram.messenger.feature.media.fileref.data.mapper

import org.telegram.messenger.FileRefController
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefParentType

object FileRefMapper {

    fun resolveParentKey(parentObject: Any?): String? {
        if (parentObject == null) return null
        return try {
            FileRefController.getKeyForParentObject(parentObject)
        } catch (_: Throwable) {
            parentObject.toString()
        }
    }

    fun classifyParentType(parentKey: String?): FileRefParentType {
        if (parentKey == null) return FileRefParentType.CUSTOM
        return when {
            parentKey.startsWith("message") -> FileRefParentType.MESSAGE
            parentKey.startsWith("story_") || parentKey.startsWith("botstory_") -> FileRefParentType.STORY
            parentKey.startsWith("user") -> FileRefParentType.USER
            parentKey.startsWith("chat") -> FileRefParentType.CHAT
            parentKey.startsWith("wallpaper") -> FileRefParentType.WALLPAPER
            parentKey.startsWith("theme") -> FileRefParentType.THEME
            parentKey.startsWith("set") -> FileRefParentType.STICKER_SET
            parentKey.startsWith("saved_gif") -> FileRefParentType.SAVED_GIF
            parentKey.startsWith("bot_info_") -> FileRefParentType.BOT_INFO
            parentKey.startsWith("attach_menu_bot_") -> FileRefParentType.ATTACH_MENU_BOT
            parentKey.startsWith("premium_promo") -> FileRefParentType.PREMIUM_PROMO
            parentKey.startsWith("available_reaction_") -> FileRefParentType.AVAILABLE_REACTION
            else -> FileRefParentType.CUSTOM
        }
    }
}
