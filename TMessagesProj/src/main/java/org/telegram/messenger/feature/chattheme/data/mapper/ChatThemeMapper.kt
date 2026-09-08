package org.telegram.messenger.feature.chattheme.data.mapper

import android.util.SparseIntArray
import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.EmojiThemes

object ChatThemeMapper {

    fun toDomain(emojiThemes: EmojiThemes?, account: Int = 0): ChatThemeModel? {
        if (emojiThemes == null) return null

        val emoticon = emojiThemes.emoji ?: emojiThemes.key?.emoticon
        val giftSlug = emojiThemes.key?.giftSlug
        val isDefault = emojiThemes.isAnyStub
        val themeId = try {
            if (emojiThemes.items.isNotEmpty()) emojiThemes.getThemeId(0) else 0L
        } catch (_: Throwable) {
            0L
        }

        val previewColorsList = mutableListOf<Int>()
        try {
            val colors: SparseIntArray? = emojiThemes.getPreviewColors(account, 0)
            if (colors != null) {
                for (i in 0 until colors.size()) {
                    previewColorsList.add(colors.valueAt(i))
                }
            }
        } catch (_: Throwable) {
            // Ignore preview color loading errors in legacy structures
        }

        return ChatThemeModel(
            emoticon = emoticon,
            giftSlug = giftSlug,
            isDefault = isDefault,
            themeId = themeId,
            previewColors = previewColorsList
        )
    }

    fun toDomainList(themes: List<EmojiThemes>?, account: Int = 0): List<ChatThemeModel> {
        if (themes.isNullOrEmpty()) return emptyList()
        return themes.mapNotNull { toDomain(it, account) }
    }

    fun toDialogThemeState(
        dialogId: Long,
        emojiThemes: EmojiThemes?,
        wallpaper: TLRPC.WallPaper?,
        account: Int = 0
    ): DialogThemeStateModel {
        val currentTheme = toDomain(emojiThemes, account)
        val wallpaperSlug = wallpaper?.slug
        val wallpaperId = wallpaper?.id

        return DialogThemeStateModel(
            dialogId = dialogId,
            currentTheme = currentTheme,
            wallpaperSlug = wallpaperSlug,
            wallpaperId = wallpaperId
        )
    }
}
