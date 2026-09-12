package org.telegram.messenger.feature.messaging.chattheme.data.datasource

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ChatThemeController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.Utilities
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.EmojiThemes
import org.telegram.ui.ActionBar.theme.ThemeKey

/**
 * Local data source managing local storage, SharedPreferences, in-memory caches,
 * and legacy ChatThemeController synchronization.
 */
open class ChatThemeLocalDataSource(protected val currentAccount: Int) {

    protected open fun getSharedPreferences(): SharedPreferences? {
        return try {
            ApplicationLoader.applicationContext?.getSharedPreferences("chatthemeconfig_$currentAccount", Context.MODE_PRIVATE)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getThemesHash(): Long {
        return try {
            getSharedPreferences()?.getLong("hash", 0L) ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    open fun getLastReloadTimeMs(): Long {
        return try {
            getSharedPreferences()?.getLong("lastReload", 0L) ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    open fun saveThemesToPrefs(themes: List<TLRPC.TL_theme>, hash: Long, lastReloadTimeMs: Long) {
        val prefs = getSharedPreferences() ?: return
        try {
            val editor = prefs.edit()
            editor.clear()
            editor.putLong("hash", hash)
            editor.putLong("lastReload", lastReloadTimeMs)
            editor.putInt("count", themes.size)
            for (i in themes.indices) {
                val theme = themes[i]
                val data = SerializedData(theme.objectSize)
                theme.serializeToStream(data)
                editor.putString("theme_$i", Utilities.bytesToHex(data.toByteArray()))
                data.cleanup()
            }
            editor.apply()
        } catch (_: Throwable) {
        }
    }

    open fun getDialogTheme(dialogId: Long): EmojiThemes? {
        return try {
            ChatThemeController.getInstance(currentAccount).getDialogTheme(dialogId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getDialogWallpaper(dialogId: Long): TLRPC.WallPaper? {
        return try {
            ChatThemeController.getInstance(currentAccount).getDialogWallpaper(dialogId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getEmojiThemes(withDefault: Boolean): List<EmojiThemes> {
        return try {
            val flags = (if (withDefault) ChatThemeController.THEME_LIST_WITH_DEFAULT else 0) or
                ChatThemeController.THEME_LIST_WITH_EMOJI or
                ChatThemeController.THEME_LIST_WITH_GIFTS
            ChatThemeController.getInstance(currentAccount).getEmojiThemes(flags) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun getInputPeer(dialogId: Long): TLRPC.InputPeer? {
        return try {
            MessagesController.getInstance(currentAccount).getInputPeer(dialogId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun setDialogTheme(dialogId: Long, themeKey: ThemeKey?) {
        try {
            ChatThemeController.getInstance(currentAccount).setDialogTheme(dialogId, themeKey)
        } catch (_: Throwable) {
        }
    }

    open fun saveChatWallpaper(dialogId: Long, wallPaper: TLRPC.WallPaper?) {
        try {
            ChatThemeController.getInstance(currentAccount).saveChatWallpaper(dialogId, wallPaper)
        } catch (_: Throwable) {
        }
    }

    open suspend fun processUpdates(updates: TLRPC.Updates) {
        withContext(Dispatchers.Main) {
            try {
                MessagesController.getInstance(currentAccount).processUpdates(updates, false)
            } catch (_: Throwable) {
            }
        }
    }
}
