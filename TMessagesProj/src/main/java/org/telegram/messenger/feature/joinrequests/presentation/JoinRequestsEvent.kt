package org.telegram.messenger.feature.joinrequests.presentation

sealed interface JoinRequestsEvent {
    data class Load(val chatId: Long) : JoinRequestsEvent
    object LoadMore : JoinRequestsEvent
    data class Search(val query: String) : JoinRequestsEvent
    data class Approve(val userId: Long) : JoinRequestsEvent
    data class Dismiss(val userId: Long) : JoinRequestsEvent
    data class ApproveAll(val inviteLink: String? = null) : JoinRequestsEvent
    data class DismissAll(val inviteLink: String? = null) : JoinRequestsEvent
    object ClearMessages : JoinRequestsEvent
}
