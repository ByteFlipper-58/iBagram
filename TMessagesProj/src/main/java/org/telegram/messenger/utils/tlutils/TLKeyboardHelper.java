package org.telegram.messenger.utils.tlutils;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_keyboard;

public class TLKeyboardHelper {
    private TLKeyboardHelper() {

    }

    public static org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository getBotKeyboardRepository(int account) {
        return org.telegram.messenger.core.di.AccountFeatureContainer.Companion.get(account).getBotKeyboardRepository();
    }

    public static org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository getRepository() {
        return getBotKeyboardRepository(org.telegram.messenger.UserConfig.selectedAccount);
    }

    public static boolean isButtonWebView(TL_keyboard.KeyboardButtonProto button) {
        return TLKeyboardHelper.isType(button, TL_keyboard.TL_inlineButtonTypeWebView.class)
            || TLKeyboardHelper.isType(button, TL_keyboard.TL_buttonTypeSimpleWebView.class);
    }

    public static <T extends TL_keyboard.ButtonTypeProto> boolean isType(TL_keyboard.KeyboardButtonProto button, Class<T> tClass) {
        return getType(button, tClass) != null;
    }

    public static <T extends TL_keyboard.ButtonTypeProto> T getType(TL_keyboard.KeyboardButtonProto button, Class<T> tClass) {
        if (button == null || tClass == null) {
            return null;
        }

        final TL_keyboard.ButtonTypeProto type = button.getType();
        if (tClass.isInstance(type)) {
            return tClass.cast(type);
        }

        return null;
    }

    public static boolean isForceReply(TLRPC.ReplyMarkup replyMarkup) {
        if (replyMarkup == null) {
            return false;
        }
        if (replyMarkup instanceof TLRPC.TL_replyKeyboardForceReply) {
            return true;
        }
        if (replyMarkup instanceof TLRPC.TL_replyInlineMarkup) {
            return replyMarkup.force_reply;
        }
        if (replyMarkup instanceof TLRPC.TL_replyKeyboardMarkup) {
            return replyMarkup.force_reply;
        }
        return false;
    }
}
