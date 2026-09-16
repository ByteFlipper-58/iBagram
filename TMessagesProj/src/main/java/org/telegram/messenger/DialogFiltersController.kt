package org.telegram.messenger

import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector
import org.telegram.ui.Components.SwipeGestureSettingsView
import java.util.ArrayList

/**
 * Controller extracted from MessagesController to modularize Dialog Filters / Chat Folders management.
 * Written in Kotlin to drive the codebase migration towards modern language standards while
 * preserving 100% Java interop and bridging directly into FoldersRepository.
 */
class DialogFiltersController(currentAccount: Int) : BaseController(currentAccount) {

    companion object {
        @JvmStatic
        private val instances = arrayOfNulls<DialogFiltersController>(UserConfig.MAX_ACCOUNT_COUNT)

        @JvmStatic
        fun getInstance(accountNum: Int): DialogFiltersController {
            var local = instances[accountNum]
            if (local == null) {
                synchronized(DialogFiltersController::class.java) {
                    local = instances[accountNum]
                    if (local == null) {
                        local = DialogFiltersController(accountNum)
                        instances[accountNum] = local
                    }
                }
            }
            return local!!
        }

        @JvmStatic
        fun getFoldersRepository(account: Int): FoldersRepository {
            return AccountFeatureContainer.get(account).messaging.foldersRepository
        }
    }

    val foldersRepository: FoldersRepository
        get() = getFoldersRepository(currentAccount)

    fun loadSuggestedFilters() {
        val mc = messagesController
        if (mc.loadingSuggestedFilters) {
            return
        }
        mc.loadingSuggestedFilters = true

        val req = TLRPC.TL_messages_getSuggestedDialogFilters()
        connectionsManager.sendRequest(req) { response, _ ->
            AndroidUtilities.runOnUIThread {
                mc.loadingSuggestedFilters = false
                mc.suggestedFilters.clear()
                if (response is Vector<*>) {
                    @Suppress("UNCHECKED_CAST")
                    mc.suggestedFilters.addAll((response as Vector<TLRPC.TL_dialogFilterSuggested>).objects)
                }
                notificationCenter.postNotificationName(NotificationCenter.suggestedFiltersLoaded)
            }
        }
    }

    @JvmOverloads
    fun loadRemoteFilters(force: Boolean, whenDone: Utilities.Callback<Boolean>? = null) {
        val mc = messagesController
        if (whenDone != null) {
            mc.onLoadedRemoteFilters = whenDone
        }
        if (mc.loadingRemoteFilters || !userConfig.isClientActivated || (!force && userConfig.filtersLoaded)) {
            return
        }
        if (force) {
            userConfig.filtersLoaded = false
            userConfig.saveConfig(false)
        }
        val req = TLRPC.TL_messages_getDialogFilters()
        connectionsManager.sendRequest(req) { response, _ ->
            if (response is Vector<*>) {
                val filters = ArrayList<TLRPC.DialogFilter>()
                val vector = response as Vector<*>
                for (i in 0 until vector.objects.size) {
                    filters.add(vector.objects[i] as TLRPC.DialogFilter)
                }
                messagesStorage.checkLoadedRemoteFilters(filters) {
                    mc.onLoadedRemoteFilters?.run(true)
                    mc.onLoadedRemoteFilters = null
                }
            } else if (response is TLRPC.TL_messages_dialogFilters) {
                val res = response as TLRPC.TL_messages_dialogFilters
                if (mc.folderTags != res.tags_enabled) {
                    mc.setFolderTags(res.tags_enabled)
                    AndroidUtilities.runOnUIThread {
                        notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)
                    }
                }
                messagesStorage.checkLoadedRemoteFilters(res.filters) {
                    mc.onLoadedRemoteFilters?.run(true)
                    mc.onLoadedRemoteFilters = null
                }
            } else {
                AndroidUtilities.runOnUIThread {
                    mc.loadingRemoteFilters = false
                    mc.onLoadedRemoteFilters?.run(false)
                    mc.onLoadedRemoteFilters = null
                }
            }
        }
    }

    fun selectDialogFilter(filter: MessagesController.DialogFilter?, index: Int) {
        val mc = messagesController
        if (mc.selectedDialogFilter[index] == filter) {
            return
        }
        val prevFilter = mc.selectedDialogFilter[index]
        mc.selectedDialogFilter[index] = filter
        val otherIndex = if (index == 0) 1 else 0
        if (mc.selectedDialogFilter[otherIndex] == filter) {
            mc.selectedDialogFilter[otherIndex] = null
        }
        if (mc.selectedDialogFilter[index] == null) {
            if (prevFilter != null) {
                prevFilter.dialogs.clear()
                prevFilter.dialogsForward.clear()
            }
        } else {
            mc.sortDialogs(null)
        }
    }

    fun onFilterUpdate(filter: MessagesController.DialogFilter?) {
        val mc = messagesController
        for (a in 0..1) {
            if (mc.selectedDialogFilter[a] == filter) {
                mc.sortDialogs(null)
                notificationCenter.postNotificationName(NotificationCenter.dialogsNeedReload, true)
                break
            }
        }
    }

    fun addFilter(filter: MessagesController.DialogFilter, atBegin: Boolean) {
        val mc = messagesController
        if (atBegin) {
            var order = 254
            for (a in 0 until mc.dialogFilters.size) {
                order = Math.min(order, mc.dialogFilters[a].order)
            }
            filter.order = order - 1
            if (mc.dialogFilters[0].isDefault) {
                mc.dialogFilters.add(1, filter)
            } else {
                mc.dialogFilters.add(0, filter)
            }
        } else {
            var order = 0
            for (a in 0 until mc.dialogFilters.size) {
                order = Math.max(order, mc.dialogFilters[a].order)
            }
            filter.order = order + 1
            mc.dialogFilters.add(filter)
        }
        mc.dialogFiltersById.put(filter.id, filter)
        if (mc.dialogFilters.size == 1 && SharedConfig.getChatSwipeAction(currentAccount) != SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS) {
            SharedConfig.updateChatListSwipeSetting(SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS)
        }
        lockFiltersInternal()
    }

    fun removeFilter(filter: MessagesController.DialogFilter) {
        val mc = messagesController
        mc.dialogFilters.remove(filter)
        mc.dialogFiltersById.remove(filter.id)
        notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)
    }

    fun lockFiltersInternal() {
        val mc = messagesController
        var changed = false
        if (!userConfig.isPremium && mc.dialogFilters.size - 1 > mc.dialogFiltersLimitDefault) {
            val n = mc.dialogFilters.size - 1 - mc.dialogFiltersLimitDefault
            val filtersSortedById = ArrayList(mc.dialogFilters)
            filtersSortedById.reverse()
            for (i in 0 until filtersSortedById.size) {
                if (i < n) {
                    if (!filtersSortedById[i].locked) {
                        changed = true
                    }
                    filtersSortedById[i].locked = true
                } else {
                    if (filtersSortedById[i].locked) {
                        changed = true
                    }
                    filtersSortedById[i].locked = false
                }
            }
        }
        if (changed) {
            notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)
        }
    }
}
