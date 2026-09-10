package org.telegram.messenger.feature.business.quickreplies.domain.model

data class QuickRepliesLimitModel(
    val currentCount: Int,
    val maxLimit: Int,
    val canAddNew: Boolean
)
