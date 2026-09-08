package org.telegram.messenger.feature.reactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.usecase.ClearReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetDoubleTapReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetReactionsSettingsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.LoadAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendVoteUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SetDoubleTapReactionUseCase

class ReactionsViewModel(
    private val observeAvailableReactionsUseCase: ObserveAvailableReactionsUseCase,
    private val getAvailableReactionsUseCase: GetAvailableReactionsUseCase,
    private val loadAvailableReactionsUseCase: LoadAvailableReactionsUseCase,
    private val observeRecentReactionsUseCase: ObserveRecentReactionsUseCase,
    private val getRecentReactionsUseCase: GetRecentReactionsUseCase,
    private val getReactionsSettingsUseCase: GetReactionsSettingsUseCase,
    private val getDoubleTapReactionUseCase: GetDoubleTapReactionUseCase,
    private val setDoubleTapReactionUseCase: SetDoubleTapReactionUseCase,
    private val sendReactionUseCase: SendReactionUseCase,
    private val clearReactionsUseCase: ClearReactionsUseCase,
    private val sendVoteUseCase: SendVoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReactionsUiState())
    val uiState: StateFlow<ReactionsUiState> = _uiState.asStateFlow()

    init {
        observeAvailableReactionsUseCase()
            .onEach { available ->
                _uiState.update { current ->
                    current.copy(availableReactions = available)
                }
            }
            .launchIn(viewModelScope)

        observeRecentReactionsUseCase()
            .onEach { recent ->
                _uiState.update { current ->
                    current.copy(recentReactions = recent)
                }
            }
            .launchIn(viewModelScope)

        loadSettings()
    }

    fun onEvent(event: ReactionsEvent) {
        when (event) {
            is ReactionsEvent.LoadAvailableReactions -> loadAvailableReactions(event.force)
            is ReactionsEvent.LoadRecentReactions -> loadRecentReactions()
            is ReactionsEvent.LoadSettings -> loadSettings()
            is ReactionsEvent.SetDoubleTapReaction -> setDoubleTapReaction(event.reaction)
            is ReactionsEvent.SendReaction -> sendReaction(
                event.dialogId,
                event.messageId,
                event.reaction,
                event.isBig,
                event.addToRecent
            )
            is ReactionsEvent.SendMultipleReactions -> sendMultipleReactions(
                event.dialogId,
                event.messageId,
                event.reactions,
                event.isBig,
                event.addToRecent
            )
            is ReactionsEvent.ClearReactions -> clearReactions(event.dialogId, event.messageId)
            is ReactionsEvent.SendVote -> sendVote(
                event.dialogId,
                event.messageId,
                event.pollId,
                event.options
            )
            is ReactionsEvent.ClearMessages -> clearMessages()
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            when (val result = getReactionsSettingsUseCase()) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            settings = result.data,
                            doubleTapReaction = result.data.doubleTapReaction
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun loadAvailableReactions(force: Boolean = false) {
        viewModelScope.launch {
            when (val result = loadAvailableReactionsUseCase(force)) {
                is Result.Success -> {
                    _uiState.update { it.copy(availableReactions = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun loadRecentReactions() {
        viewModelScope.launch {
            when (val result = getRecentReactionsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(recentReactions = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun setDoubleTapReaction(reaction: String) {
        viewModelScope.launch {
            when (val result = setDoubleTapReactionUseCase(reaction)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            doubleTapReaction = reaction,
                            settings = current.settings.copy(doubleTapReaction = reaction),
                            actionSuccessMessage = "Quick reaction updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun sendReaction(
        dialogId: Long,
        messageId: Int,
        reaction: ReactionItemModel,
        isBig: Boolean = false,
        addToRecent: Boolean = true
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = sendReactionUseCase.single(dialogId, messageId, reaction, isBig, addToRecent)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedRecent = if (addToRecent && !current.recentReactions.any { it.reaction == reaction.reaction }) {
                            listOf(reaction) + current.recentReactions
                        } else current.recentReactions

                        current.copy(
                            recentReactions = updatedRecent,
                            isLoading = false,
                            actionSuccessMessage = "Reaction sent"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun sendMultipleReactions(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean = false,
        addToRecent: Boolean = true
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = sendReactionUseCase(dialogId, messageId, reactions, isBig, addToRecent)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccessMessage = "Reactions sent") }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun clearReactions(dialogId: Long, messageId: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = clearReactionsUseCase(dialogId, messageId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccessMessage = "Reactions cleared") }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun sendVote(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = sendVoteUseCase(dialogId, messageId, pollId, options)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccessMessage = "Vote recorded") }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
