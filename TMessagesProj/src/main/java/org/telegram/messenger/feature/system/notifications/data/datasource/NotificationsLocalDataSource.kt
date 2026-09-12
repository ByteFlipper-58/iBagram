package org.telegram.messenger.feature.system.notifications.data.datasource

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.tgnet.ConnectionsManager

/**
 * Local data source managing notification settings persistence in SharedPreferences,
 * SQLite storage via MessagesStorage on Dispatchers.IO, and in-memory caches of NotificationsController.
 */
open class NotificationsLocalDataSource(
    currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseLocalDataSource(currentAccount, ioDispatcher) {

    private val notificationsController: NotificationsController?
        get() = try {
            NotificationsController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val connectionsManager: ConnectionsManager?
        get() = try {
            ConnectionsManager.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    /**
     * Obtains SharedPreferences instance for notification settings safely.
     */
    open fun getPreferences(): SharedPreferences? {
        return try {
            MessagesController.getNotificationsSettings(currentAccount)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Gets current synchronized server time or local timestamp fallback.
     */
    open fun getCurrentTime(): Int {
        return try {
            connectionsManager?.currentTime ?: (System.currentTimeMillis() / 1000).toInt()
        } catch (e: Throwable) {
            (System.currentTimeMillis() / 1000).toInt()
        }
    }

    /**
     * Gets boolean value from SharedPreferences.
     */
    open fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getPreferences()?.getBoolean(key, defaultValue) ?: defaultValue
    }

    /**
     * Gets integer value from SharedPreferences.
     */
    open fun getInt(key: String, defaultValue: Int): Int {
        return getPreferences()?.getInt(key, defaultValue) ?: defaultValue
    }

    /**
     * Gets long value from SharedPreferences.
     */
    open fun getLong(key: String, defaultValue: Long): Long {
        return getPreferences()?.getLong(key, defaultValue) ?: defaultValue
    }

    /**
     * Gets string value from SharedPreferences.
     */
    open fun getString(key: String, defaultValue: String?): String? {
        return getPreferences()?.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Saves boolean value in SharedPreferences synchronously.
     */
    open fun putBoolean(key: String, value: Boolean): Boolean {
        val editor = getPreferences()?.edit() ?: return false
        return editor.putBoolean(key, value).commit()
    }

    /**
     * Saves integer value in SharedPreferences synchronously.
     */
    open fun putInt(key: String, value: Int): Boolean {
        val editor = getPreferences()?.edit() ?: return false
        return editor.putInt(key, value).commit()
    }

    /**
     * Removes key from SharedPreferences synchronously.
     */
    open fun removeKey(key: String): Boolean {
        val editor = getPreferences()?.edit() ?: return false
        return editor.remove(key).commit()
    }

    /**
     * Returns total unread messages count from NotificationsController in-memory cache.
     */
    open fun getTotalUnreadCount(): Int {
        return notificationsController?.totalAllUnreadCount ?: 0
    }

    /**
     * Returns whether badge number is enabled.
     */
    open fun getShowBadgeNumber(): Boolean {
        return getBoolean("badgeNumber", notificationsController?.showBadgeNumber ?: true)
    }

    /**
     * Returns whether muted chats are included in badge count.
     */
    open fun getShowBadgeMuted(): Boolean {
        return getBoolean("badgeNumberMuted", notificationsController?.showBadgeMuted ?: false)
    }

    /**
     * Returns whether individual messages or chats are counted in badge.
     */
    open fun getShowBadgeMessages(): Boolean {
        return getBoolean("badgeNumberMessages", notificationsController?.showBadgeMessages ?: true)
    }

    /**
     * Updates badge configuration in memory and SharedPreferences.
     */
    open fun setBadgeSettings(showNumber: Boolean, showMuted: Boolean, showMessages: Boolean) {
        notificationsController?.let {
            it.showBadgeNumber = showNumber
            it.showBadgeMuted = showMuted
            it.showBadgeMessages = showMessages
        }
        getPreferences()?.edit()?.apply {
            putBoolean("badgeNumber", showNumber)
            putBoolean("badgeNumberMuted", showMuted)
            putBoolean("badgeNumberMessages", showMessages)
            commit()
        }
    }

    /**
     * Triggers in-memory badge count recalculation and update.
     */
    open fun updateBadge() {
        notificationsController?.updateBadge()
    }

    /**
     * Sets in-chat sound enabled flag in controller and preferences.
     */
    open fun setInChatSoundEnabled(enabled: Boolean) {
        putBoolean("EnableInChatSound", enabled)
        notificationsController?.setInChatSoundEnabled(enabled)
    }

    /**
     * Sets global notifications enabled duration for a chat category.
     */
    open fun setGlobalNotificationsEnabled(type: Int, time: Int) {
        notificationsController?.setGlobalNotificationsEnabled(type, time)
    }

    /**
     * Mutes or unmutes a specific dialog / topic.
     */
    open fun muteDialog(dialogId: Long, topicId: Long, mute: Boolean) {
        notificationsController?.muteDialog(dialogId, topicId, mute)
    }

    /**
     * Mutes a dialog until a specific unix timestamp.
     */
    open fun muteDialogUntil(dialogId: Long, topicId: Long, untilDate: Int) {
        notificationsController?.muteUntil(dialogId, topicId, untilDate)
    }

    /**
     * Checks if a dialog / topic is currently muted.
     */
    open fun isDialogMuted(dialogId: Long, topicId: Long): Boolean {
        return messagesController?.isDialogMuted(dialogId, topicId) ?: false
    }

    /**
     * Updates muted dialog filter counters in MessagesStorage on Dispatchers.IO.
     */
    open suspend fun updateMutedDialogsFiltersCounters() = withContext(ioDispatcher) {
        try {
            messagesStorage.updateMutedDialogsFiltersCounters()
        } catch (e: Throwable) {
            // Ignored when running outside Android SQLite runtime
        }
    }

    /**
     * Updates dialog flags in MessagesStorage database on Dispatchers.IO.
     */
    open suspend fun setDialogFlags(dialogId: Long, flags: Long) = withContext(ioDispatcher) {
        try {
            messagesStorage.setDialogFlags(dialogId, flags)
        } catch (e: Throwable) {
            // Ignored when running outside Android SQLite runtime
        }
    }
}
