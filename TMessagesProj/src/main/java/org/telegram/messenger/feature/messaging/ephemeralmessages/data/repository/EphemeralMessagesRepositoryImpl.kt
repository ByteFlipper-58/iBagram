package org.telegram.messenger.feature.messaging.ephemeralmessages.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.datasource.EphemeralMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.datasource.EphemeralMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralBotCommandInfo
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model.EphemeralMessagesState
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.repository.EphemeralMessagesRepository

/**
 * Modern Clean Architecture implementation of [EphemeralMessagesRepository]
 * coordinating local anchor bindings and remote MTProto interactions.
 */
open class EphemeralMessagesRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: EphemeralMessagesLocalDataSource,
    private val remoteDataSource: EphemeralMessagesRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : EphemeralMessagesRepository {

    override fun getEphemeralCommandBotId(text: String, dialogId: Long): Long {
        return localDataSource.getEphemeralCommandBotId(text, dialogId)
    }

    override fun isEphemeralCommand(text: String, dialogId: Long): Boolean {
        return localDataSource.isEphemeralCommand(text, dialogId)
    }

    override fun parseCommand(text: String): EphemeralBotCommandInfo? {
        return localDataSource.parseCommand(text)
    }

    override fun putAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        localDataSource.putAnchorBinding(dialogId, messageId, ephemeralMessageId)
    }

    override fun removeAnchorBinding(dialogId: Long, messageId: Int, ephemeralMessageId: Int) {
        localDataSource.removeAnchorBinding(dialogId, messageId, ephemeralMessageId)
    }

    override fun getAnchorBindings(dialogId: Long): Map<Int, Int> {
        return localDataSource.getAnchorBindings(dialogId)
    }

    override fun clearAnchorBindings(dialogId: Long) {
        localDataSource.clearAnchorBindings(dialogId)
    }

    override fun clearAllAnchorBindings() {
        localDataSource.clearAllAnchorBindings()
    }

    override fun observeState(): StateFlow<EphemeralMessagesState> {
        return localDataSource.state
    }

    override fun getCurrentState(): EphemeralMessagesState {
        return localDataSource.state.value
    }
}
