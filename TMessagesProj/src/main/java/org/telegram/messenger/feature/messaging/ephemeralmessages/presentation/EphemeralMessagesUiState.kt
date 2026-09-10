package org.telegram.messenger.feature.messaging.ephemeralmessages.presentation

import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralBotCommandInfo

data class EphemeralMessagesUiState(
    val currentDialogId: Long = 0L,
    val isCurrentInputEphemeral: Boolean = false,
    val currentCommandInfo: EphemeralBotCommandInfo? = null,
    val activeAnchorBindings: Map<Int, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
