package org.telegram.messenger.feature.ephemeralmessages.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralBotCommandInfo
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralMessageIdHelper
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralMessagesState
import org.telegram.messenger.feature.ephemeralmessages.domain.repository.EphemeralMessagesRepository

class ParseBotCommandUseCase {
    operator fun invoke(text: String): EphemeralBotCommandInfo? {
        if (!text.startsWith("/") || text.length < 2) return null

        val body = if (text.contains(' ')) {
            text.substring(1, text.indexOf(' '))
        } else {
            text.substring(1)
        }

        if (body.isEmpty()) return null

        val atIdx = body.indexOf('@')
        return if (atIdx != -1) {
            val command = body.substring(0, atIdx)
            val username = body.substring(atIdx + 1)
            if (command.isEmpty()) null else EphemeralBotCommandInfo(command = command, botUsername = username)
        } else {
            EphemeralBotCommandInfo(command = body, botUsername = null)
        }
    }
}

class GetEphemeralCommandBotIdUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(text: String, dialogId: Long): Long {
        return repository.getEphemeralCommandBotId(text, dialogId)
    }
}

class IsEphemeralCommandUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(text: String, dialogId: Long): Boolean {
        return repository.isEphemeralCommand(text, dialogId)
    }
}

class PackEphemeralMessageIdUseCase {
    operator fun invoke(id: Int): Int {
        return EphemeralMessageIdHelper.pack(id)
    }
}

class UnpackEphemeralMessageIdUseCase {
    operator fun invoke(packedId: Int): Int {
        return EphemeralMessageIdHelper.unpack(packedId)
    }
}

class IsEphemeralMessageIdUseCase {
    operator fun invoke(id: Int): Boolean {
        return EphemeralMessageIdHelper.isEphemeral(id)
    }
}

class PutWelcomeAnchorBindingUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        repository.putAnchorBinding(dialogId, messageId, ephemeralMessageId)
    }
}

class RemoveWelcomeAnchorBindingUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        repository.removeAnchorBinding(dialogId, messageId, ephemeralMessageId)
    }
}

class GetWelcomeAnchorBindingsUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(dialogId: Long): Map<Int, Int> {
        return repository.getAnchorBindings(dialogId)
    }
}

class ClearAllWelcomeAnchorBindingsUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke() {
        repository.clearAllAnchorBindings()
    }
}

class ObserveEphemeralMessagesStateUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(): StateFlow<EphemeralMessagesState> {
        return repository.observeState()
    }
}

class GetEphemeralMessagesStateUseCase(
    private val repository: EphemeralMessagesRepository
) {
    operator fun invoke(): EphemeralMessagesState {
        return repository.getCurrentState()
    }
}
