package org.telegram.messenger.feature.business.botstars.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetAdminedBotsAndChannelsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetTonStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadSuggestedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveTonStatsUseCase

class BotStarsViewModel(
    private val observeBotStarsStatsUseCase: ObserveBotStarsStatsUseCase,
    private val getBotStarsStatsUseCase: GetBotStarsStatsUseCase,
    private val observeTonStatsUseCase: ObserveTonStatsUseCase,
    private val getTonStatsUseCase: GetTonStatsUseCase,
    private val observeBotTransactionsUseCase: ObserveBotTransactionsUseCase,
    private val loadBotTransactionsUseCase: LoadBotTransactionsUseCase,
    private val observeConnectedStarBotsUseCase: ObserveConnectedStarBotsUseCase,
    private val loadConnectedStarBotsUseCase: LoadConnectedStarBotsUseCase,
    private val loadSuggestedStarBotsUseCase: LoadSuggestedStarBotsUseCase,
    private val getAdminedBotsAndChannelsUseCase: GetAdminedBotsAndChannelsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotStarsUiState())
    val uiState: StateFlow<BotStarsUiState> = _uiState.asStateFlow()

    private var statsJob: Job? = null
    private var tonJob: Job? = null
    private var txsJob: Job? = null
    private var connectedBotsJob: Job? = null

    fun onEvent(event: BotStarsEvent) {
        when (event) {
            is BotStarsEvent.SetDialogId -> setDialogId(event.dialogId)
            is BotStarsEvent.RefreshStats -> refreshStats(event.force)
            is BotStarsEvent.SelectTransactionType -> selectTransactionType(event.type)
            is BotStarsEvent.LoadTransactions -> loadTransactions(event.reload)
            is BotStarsEvent.LoadConnectedBots -> loadConnectedBots(event.reload)
            is BotStarsEvent.LoadSuggestedBots -> loadSuggestedBots(event.sort)
            is BotStarsEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun setDialogId(dialogId: Long) {
        if (_uiState.value.dialogId == dialogId) return
        _uiState.update { it.copy(dialogId = dialogId) }

        statsJob?.cancel()
        tonJob?.cancel()
        txsJob?.cancel()
        connectedBotsJob?.cancel()

        statsJob = viewModelScope.launch {
            observeBotStarsStatsUseCase(dialogId).collect { stats ->
                _uiState.update { it.copy(starsStats = stats) }
            }
        }

        tonJob = viewModelScope.launch {
            observeTonStatsUseCase(dialogId).collect { stats ->
                _uiState.update { it.copy(tonStats = stats) }
            }
        }

        observeTransactions(dialogId, _uiState.value.selectedTransactionType)

        connectedBotsJob = viewModelScope.launch {
            observeConnectedStarBotsUseCase(dialogId).collect { bots ->
                _uiState.update { it.copy(connectedBots = bots) }
            }
        }
    }

    private fun observeTransactions(dialogId: Long, type: BotStarsTransactionType) {
        txsJob?.cancel()
        txsJob = viewModelScope.launch {
            observeBotTransactionsUseCase(dialogId, type).collect { txs ->
                _uiState.update { it.copy(transactions = txs) }
            }
        }
    }

    fun selectTransactionType(type: BotStarsTransactionType) {
        _uiState.update { it.copy(selectedTransactionType = type) }
        val dialogId = _uiState.value.dialogId
        if (dialogId != 0L) {
            observeTransactions(dialogId, type)
            loadTransactions(false)
        }
    }

    fun refreshStats(force: Boolean = true) {
        val dialogId = _uiState.value.dialogId
        if (dialogId == 0L) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val starsResult = getBotStarsStatsUseCase(dialogId, force)
            val tonResult = getTonStatsUseCase(dialogId, force)

            _uiState.update { current ->
                val err = when {
                    starsResult is Result.Failure -> starsResult.error.message
                    tonResult is Result.Failure -> tonResult.error.message
                    else -> null
                }
                current.copy(
                    isLoading = false,
                    errorMessage = err,
                    starsStats = (starsResult as? Result.Success)?.data ?: current.starsStats,
                    tonStats = (tonResult as? Result.Success)?.data ?: current.tonStats
                )
            }
        }
    }

    fun loadTransactions(reload: Boolean = false) {
        val dialogId = _uiState.value.dialogId
        if (dialogId == 0L) return
        viewModelScope.launch {
            when (val res = loadBotTransactionsUseCase(dialogId, _uiState.value.selectedTransactionType, reload)) {
                is Result.Success -> _uiState.update { it.copy(transactions = res.data) }
                is Result.Failure -> _uiState.update { it.copy(errorMessage = res.error.message) }
            }
        }
    }

    fun loadConnectedBots(reload: Boolean = false) {
        val dialogId = _uiState.value.dialogId
        if (dialogId == 0L) return
        viewModelScope.launch {
            when (val res = loadConnectedStarBotsUseCase(dialogId, reload)) {
                is Result.Success -> _uiState.update { it.copy(connectedBots = res.data) }
                is Result.Failure -> _uiState.update { it.copy(errorMessage = res.error.message) }
            }
        }
    }

    fun loadSuggestedBots(sort: Int = 0) {
        val dialogId = _uiState.value.dialogId
        if (dialogId == 0L) return
        viewModelScope.launch {
            when (val res = loadSuggestedStarBotsUseCase(dialogId, sort)) {
                is Result.Success -> _uiState.update { it.copy(suggestedBots = res.data) }
                is Result.Failure -> _uiState.update { it.copy(errorMessage = res.error.message) }
            }
        }
    }
}
