package org.telegram.messenger.feature.business.quickreplies.presentation

sealed interface QuickRepliesEvent {
    data class Load(val force: Boolean = false) : QuickRepliesEvent
    data class Rename(val id: Int, val newName: String) : QuickRepliesEvent
    data class Reorder(val ids: List<Int>) : QuickRepliesEvent
    data class Delete(val ids: List<Int>) : QuickRepliesEvent
    data class Send(val dialogId: Long, val shortcutId: Int) : QuickRepliesEvent
    object CheckCanAddNew : QuickRepliesEvent
    object ClearMessages : QuickRepliesEvent
}
