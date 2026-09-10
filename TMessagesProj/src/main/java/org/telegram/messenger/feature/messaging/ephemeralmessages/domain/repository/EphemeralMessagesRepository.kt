package org.telegram.messenger.feature.messaging.ephemeralmessages.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralBotCommandInfo
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralMessagesState

interface EphemeralMessagesRepository {
    fun getEphemeralCommandBotId(text: String, dialogId: Long): Long
    fun isEphemeralCommand(text: String, dialogId: Long): Boolean
    fun parseCommand(text: String): EphemeralBotCommandInfo?

    fun putAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int)
    fun removeAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int)
    fun getAnchorBindings(dialogId: Long): Map<Int, Int>
    fun clearAnchorBindings(dialogId: Long)
    fun clearAllAnchorBindings()

    fun observeState(): StateFlow<EphemeralMessagesState>
    fun getCurrentState(): EphemeralMessagesState
}
