package org.telegram.messenger.feature.social.joinrequests.domain.model

data class JoinRequestsListModel(
    val totalCount: Int,
    val requests: List<JoinRequestModel>,
    val hasMore: Boolean
)
