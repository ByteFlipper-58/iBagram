package org.telegram.messenger.feature.folders.data.mapper

import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.model.SuggestedFolderModel
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram [MessagesController.DialogFilter] and [TLRPC.TL_dialogFilterSuggested]
 * to clean domain entities [FolderModel] and [SuggestedFolderModel].
 */
object FolderMapper {

    fun toDomain(filter: MessagesController.DialogFilter?): FolderModel {
        if (filter == null) {
            return FolderModel(id = 0, name = "")
        }

        val pinnedIds = mutableListOf<Long>()
        val pinned = filter.pinnedDialogs
        if (pinned != null) {
            for (i in 0 until pinned.size()) {
                pinnedIds.add(pinned.keyAt(i))
            }
        }

        return FolderModel(
            id = filter.id,
            name = filter.name ?: "",
            unreadCount = filter.unreadCount,
            order = filter.order,
            flags = filter.flags,
            color = filter.color,
            isDefault = filter.isDefault,
            isChatlist = filter.isChatlist,
            isLocked = filter.locked,
            includedPeerIds = filter.alwaysShow?.toList() ?: emptyList(),
            excludedPeerIds = filter.neverShow?.toList() ?: emptyList(),
            pinnedPeerIds = pinnedIds
        )
    }

    fun toDomainList(filters: List<MessagesController.DialogFilter>?): List<FolderModel> {
        return filters?.map { toDomain(it) } ?: emptyList()
    }

    fun toDomain(suggested: TLRPC.TL_dialogFilterSuggested?): SuggestedFolderModel {
        if (suggested == null) {
            return SuggestedFolderModel(
                id = 0,
                description = "",
                folder = FolderModel(id = 0, name = "")
            )
        }

        val tlFilter = suggested.filter
        var flags = 0
        if (tlFilter != null) {
            if (tlFilter.groups) flags = flags or MessagesController.DIALOG_FILTER_FLAG_GROUPS
            if (tlFilter.bots) flags = flags or MessagesController.DIALOG_FILTER_FLAG_BOTS
            if (tlFilter.contacts) flags = flags or MessagesController.DIALOG_FILTER_FLAG_CONTACTS
            if (tlFilter.non_contacts) flags = flags or MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS
            if (tlFilter.broadcasts) flags = flags or MessagesController.DIALOG_FILTER_FLAG_CHANNELS
            if (tlFilter.exclude_archived) flags = flags or MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED
            if (tlFilter.exclude_read) flags = flags or MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ
            if (tlFilter.exclude_muted) flags = flags or MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED
        }

        val includedPeers = mutableListOf<Long>()
        tlFilter?.include_peers?.forEach { peer ->
            val id = when {
                peer.user_id != 0L -> peer.user_id
                peer.chat_id != 0L -> -peer.chat_id
                peer.channel_id != 0L -> -peer.channel_id
                else -> 0L
            }
            if (id != 0L) includedPeers.add(id)
        }

        val excludedPeers = mutableListOf<Long>()
        tlFilter?.exclude_peers?.forEach { peer ->
            val id = when {
                peer.user_id != 0L -> peer.user_id
                peer.chat_id != 0L -> -peer.chat_id
                peer.channel_id != 0L -> -peer.channel_id
                else -> 0L
            }
            if (id != 0L) excludedPeers.add(id)
        }

        val folderModel = FolderModel(
            id = tlFilter?.id ?: 0,
            name = tlFilter?.title?.text ?: "",
            flags = flags,
            color = tlFilter?.color ?: -1,
            includedPeerIds = includedPeers,
            excludedPeerIds = excludedPeers
        )

        return SuggestedFolderModel(
            id = suggested.filter?.id ?: 0,
            description = suggested.description ?: "",
            folder = folderModel
        )
    }

    fun toSuggestedList(suggestedList: List<TLRPC.TL_dialogFilterSuggested>?): List<SuggestedFolderModel> {
        return suggestedList?.map { toDomain(it) } ?: emptyList()
    }
}
