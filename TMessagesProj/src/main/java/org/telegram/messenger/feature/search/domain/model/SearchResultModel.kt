package org.telegram.messenger.feature.search.domain.model

data class SearchResultModel(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val username: String? = null,
    val type: SearchResultType,
    val date: Long = 0L,
    val avatarLocation: String? = null,
    val isVerified: Boolean = false,
    val isScam: Boolean = false,
    val isFake: Boolean = false,
    val chatId: Long = 0L,
    val messageId: Int = 0,
    val originalObject: Any? = null
)
