package org.telegram.messenger.feature.pushlistener.presentation

import org.telegram.messenger.feature.pushlistener.domain.model.PushType

data class FormattedPushItem(
    val pushType: String,
    val actionType: String,
    val locKey: String,
    val dialogId: Long,
    val topicId: Int,
    val isSilent: Boolean,
    val timestamp: Long
)

data class PushListenerUiState(
    val isListening: Boolean = true,
    val registeredTokens: Map<PushType, String> = emptyMap(),
    val lastPush: FormattedPushItem? = null,
    val totalReceived: Int = 0,
    val totalErrors: Int = 0,
    val infoMessage: String? = null,
    val error: String? = null
)
