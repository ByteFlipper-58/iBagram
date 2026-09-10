package org.telegram.messenger.feature.messaging.ephemeralmessages.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralBotCommandInfo
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralMessagesState
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.repository.EphemeralMessagesRepository
import org.telegram.messenger.utils.EphemeralMessagesHelper
import java.util.concurrent.ConcurrentHashMap

class LegacyEphemeralMessagesRepository(
    private val currentAccount: Int
) : EphemeralMessagesRepository {

    private val inMemoryAnchorBindings = ConcurrentHashMap<Long, ConcurrentHashMap<Int, Int>>()
    private val inMemoryBotCommands = ConcurrentHashMap<String, Long>() // "/command" or "/command@bot" -> botId
    private val inMemoryEphemeralCommands = ConcurrentHashMap.newKeySet<String>() // "/command"

    private val _state = MutableStateFlow(EphemeralMessagesState())

    override fun getEphemeralCommandBotId(text: String, dialogId: Long): Long {
        val legacyResult = runCatching {
            EphemeralMessagesHelper.getInstance(currentAccount)?.getEphemeralCommandBotId(text, dialogId)
        }.getOrNull()

        if (legacyResult != null && legacyResult > 0L) {
            return legacyResult
        }

        val parsed = parseCommand(text) ?: return 0L
        val commandKey = if (parsed.botUsername != null) {
            "/${parsed.command}@${parsed.botUsername}".lowercase()
        } else {
            "/${parsed.command}".lowercase()
        }

        if (inMemoryEphemeralCommands.contains(commandKey) || inMemoryEphemeralCommands.contains("/${parsed.command}".lowercase())) {
            return inMemoryBotCommands[commandKey] ?: inMemoryBotCommands["/${parsed.command}".lowercase()] ?: 1L
        }

        return 0L
    }

    override fun isEphemeralCommand(text: String, dialogId: Long): Boolean {
        return getEphemeralCommandBotId(text, dialogId) > 0L
    }

    override fun parseCommand(text: String): EphemeralBotCommandInfo? {
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

    fun registerTestBotCommand(command: String, botUsername: String? = null, botId: Long, isEphemeral: Boolean) {
        val key = if (botUsername != null) {
            "/$command@$botUsername".lowercase()
        } else {
            "/$command".lowercase()
        }
        inMemoryBotCommands[key] = botId
        if (isEphemeral) {
            inMemoryEphemeralCommands.add(key)
        } else {
            inMemoryEphemeralCommands.remove(key)
        }
    }

    override fun putAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        val map = inMemoryAnchorBindings.computeIfAbsent(dialogId) { ConcurrentHashMap() }
        map[messageId] = ephemeralMessageId
        syncState()
    }

    override fun removeAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        val map = inMemoryAnchorBindings[dialogId] ?: return
        if (map[messageId] == ephemeralMessageId) {
            map.remove(messageId)
            if (map.isEmpty()) {
                inMemoryAnchorBindings.remove(dialogId)
            }
            syncState()
        }
    }

    override fun getAnchorBindings(dialogId: Long): Map<Int, Int> {
        return inMemoryAnchorBindings[dialogId]?.toMap() ?: emptyMap()
    }

    override fun clearAnchorBindings(dialogId: Long) {
        inMemoryAnchorBindings.remove(dialogId)
        syncState()
    }

    override fun clearAllAnchorBindings() {
        inMemoryAnchorBindings.clear()
        syncState()
    }

    override fun observeState(): StateFlow<EphemeralMessagesState> {
        return _state.asStateFlow()
    }

    override fun getCurrentState(): EphemeralMessagesState {
        return _state.value
    }

    private fun syncState() {
        val snapshot = HashMap<Long, Map<Int, Int>>()
        for ((k, v) in inMemoryAnchorBindings) {
            snapshot[k] = HashMap(v)
        }
        _state.update { current ->
            current.copy(activeAnchorBindings = snapshot)
        }
    }
}
