package org.telegram.messenger.feature.messaging.botkeyboard.data.mapper

import org.telegram.messenger.BotInlineKeyboard
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonColor
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonTypeCategory
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotCustomButtonType
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardRow
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper
import org.telegram.tgnet.tl.TL_keyboard

object BotKeyboardMapper {

    fun mapColor(color: BotInlineKeyboard.BackgroundColor?): BotButtonColor {
        return when (color) {
            BotInlineKeyboard.BackgroundColor.PRIMARY -> BotButtonColor.PRIMARY
            BotInlineKeyboard.BackgroundColor.SUCCESS -> BotButtonColor.SUCCESS
            BotInlineKeyboard.BackgroundColor.DANGER -> BotButtonColor.DANGER
            else -> BotButtonColor.NONE
        }
    }

    fun mapButton(button: BotInlineKeyboard.Button?): BotButtonItem? {
        if (button == null) return null

        if (button is BotInlineKeyboard.ButtonCustom) {
            val customType = BotCustomButtonType.fromId(button.id) ?: BotCustomButtonType.SUGGESTION_DECLINE
            return BotButtonItem.CustomAction(
                customType = customType,
                text = button.getText() ?: "",
                color = mapColor(button.color),
                iconRes = button.iconRes
            )
        }

        if (button is BotInlineKeyboard.ButtonBot) {
            val inlineButton = button.button
            val category = resolveCategory(inlineButton)
            return BotButtonItem.BotAction(
                text = button.getText() ?: "",
                color = mapColor(button.color),
                category = category,
                iconEmoji = button.iconEmoji,
                data = inlineButton?.data,
                url = inlineButton?.url
            )
        }

        return BotButtonItem.BotAction(
            text = button.getText() ?: "",
            color = mapColor(button.color)
        )
    }

    fun resolveCategory(button: TL_keyboard.KeyboardInlineButton?): BotButtonTypeCategory {
        if (button == null) return BotButtonTypeCategory.UNKNOWN
        return when {
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeCallback::class.java) -> BotButtonTypeCategory.CALLBACK
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeUrl::class.java) -> BotButtonTypeCategory.URL
            TLKeyboardHelper.isButtonWebView(button) -> BotButtonTypeCategory.WEB_VIEW
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeBuy::class.java) -> BotButtonTypeCategory.BUY
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeGame::class.java) -> BotButtonTypeCategory.GAME
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeSwitchInline::class.java) -> BotButtonTypeCategory.SWITCH_INLINE
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeUrlAuth::class.java) -> BotButtonTypeCategory.URL_AUTH
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeCopy::class.java) -> BotButtonTypeCategory.COPY
            TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeDisabled::class.java) -> BotButtonTypeCategory.DISABLED
            else -> BotButtonTypeCategory.UNKNOWN
        }
    }

    fun mapSource(source: BotInlineKeyboard.Source?): BotKeyboardLayout {
        if (source == null || source.isEmpty) {
            return BotKeyboardLayout()
        }

        val rowsCount = source.rowsCount
        val rows = ArrayList<BotKeyboardRow>(rowsCount)

        for (r in 0 until rowsCount) {
            val colsCount = source.getColumnsCount(r)
            val buttons = ArrayList<BotButtonItem>(colsCount)
            for (c in 0 until colsCount) {
                val btn = source.getButton(r, c)
                val mapped = mapButton(btn)
                if (mapped != null) {
                    buttons.add(mapped)
                }
            }
            val hasSeparator = source.hasSeparator(r)
            rows.add(BotKeyboardRow(buttons = buttons, hasSeparator = hasSeparator))
        }

        return BotKeyboardLayout(rows = rows)
    }
}
