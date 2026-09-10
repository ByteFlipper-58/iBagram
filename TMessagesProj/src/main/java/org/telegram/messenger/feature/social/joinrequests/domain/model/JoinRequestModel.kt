package org.telegram.messenger.feature.social.joinrequests.domain.model

data class JoinRequestModel(
    val userId: Long,
    val chatId: Long,
    val date: Int,
    val about: String? = null,
    val isRequested: Boolean = true,
    val viaChatlist: Boolean = false,
    val user: JoinRequestUserModel? = null
)
