package org.telegram.messenger.feature.joinrequests.data.mapper

import org.telegram.tgnet.TLRPC
import org.telegram.messenger.feature.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestUserModel
import org.telegram.messenger.feature.joinrequests.domain.model.JoinRequestsListModel

object JoinRequestMapper {

    fun mapUser(user: TLRPC.User?): JoinRequestUserModel? {
        if (user == null) return null
        return JoinRequestUserModel(
            id = user.id,
            firstName = user.first_name ?: "",
            lastName = user.last_name ?: "",
            username = user.username,
            phone = user.phone,
            isBot = user.bot,
            isVerified = user.verified,
            isPremium = user.premium
        )
    }

    fun mapImporter(
        chatId: Long,
        importer: TLRPC.TL_chatInviteImporter,
        userMap: Map<Long, TLRPC.User> = emptyMap()
    ): JoinRequestModel {
        val user = userMap[importer.user_id]
        return JoinRequestModel(
            userId = importer.user_id,
            chatId = chatId,
            date = importer.date,
            about = importer.about,
            isRequested = importer.requested,
            viaChatlist = importer.via_chatlist,
            user = mapUser(user)
        )
    }

    fun mapImportersList(
        chatId: Long,
        importers: TLRPC.TL_messages_chatInviteImporters?,
        hasMoreFallback: Boolean = false
    ): JoinRequestsListModel {
        if (importers == null) {
            return JoinRequestsListModel(
                totalCount = 0,
                requests = emptyList(),
                hasMore = false
            )
        }
        val userMap = buildMap {
            importers.users?.forEach { user ->
                if (user != null) put(user.id, user)
            }
        }
        val mappedList = importers.importers?.mapNotNull { importer ->
            if (importer != null) mapImporter(chatId, importer, userMap) else null
        } ?: emptyList()

        val hasMore = if (importers.count > 0) {
            mappedList.size < importers.count
        } else {
            hasMoreFallback
        }

        return JoinRequestsListModel(
            totalCount = importers.count,
            requests = mappedList,
            hasMore = hasMore
        )
    }

    fun mapChatPendingRequests(chatId: Long, chatFull: TLRPC.ChatFull?): ChatPendingRequestsModel {
        if (chatFull == null) {
            return ChatPendingRequestsModel(
                chatId = chatId,
                pendingCount = 0,
                recentRequestersUserIds = emptyList()
            )
        }
        val requesters = chatFull.recent_requesters?.toList() ?: emptyList()
        return ChatPendingRequestsModel(
            chatId = chatId,
            pendingCount = chatFull.requests_pending,
            recentRequestersUserIds = requesters
        )
    }
}
