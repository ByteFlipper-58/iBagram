package org.telegram.messenger.feature.business.businessbots.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.business.businessbots.domain.usecase.DeleteConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.LoadConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.ObserveConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.UpdateConnectedBotUseCase

class BusinessBotsViewModel(
    private val observeConnectedBotsUseCase: ObserveConnectedBotsUseCase,
    private val loadConnectedBotsUseCase: LoadConnectedBotsUseCase,
    private val updateConnectedBotUseCase: UpdateConnectedBotUseCase,
    private val deleteConnectedBotUseCase: DeleteConnectedBotUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessBotsUiState())
    val uiState: StateFlow<BusinessBotsUiState> = _uiState.asStateFlow()

    init {
        observeBots()
    }

    private fun observeBots() {
        viewModelScope.launch {
            observeConnectedBotsUseCase().collect { bots ->
                _uiState.update { current ->
                    val selected = if (current.selectedBot != null) {
                        bots.find { it.botId == current.selectedBot.botId }
                    } else {
                        bots.firstOrNull()
                    }
                    current.copy(connectedBots = bots, selectedBot = selected)
                }
            }
        }
    }

    fun onEvent(event: BusinessBotsEvent) {
        when (event) {
            is BusinessBotsEvent.Load -> load(event.forceReload)
            is BusinessBotsEvent.SelectBot -> selectBot(event.bot)
            is BusinessBotsEvent.UpdateBot -> updateBot(event.botId, event.rights, event.recipients)
            is BusinessBotsEvent.DeleteBot -> deleteBot(event.botId)
            is BusinessBotsEvent.ClearMessages -> clearMessages()
        }
    }

    private fun load(forceReload: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loadConnectedBotsUseCase(forceReload)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun selectBot(bot: ConnectedBotModel?) {
        _uiState.update { it.copy(selectedBot = bot) }
    }

    private fun updateBot(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = updateConnectedBotUseCase(botId, rights, recipients)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedBot = result.data,
                        actionSuccessMessage = "Business bot updated successfully"
                    )
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun deleteBot(botId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = deleteConnectedBotUseCase(botId)) {
                is Result.Success -> _uiState.update {
                    val updatedList = it.connectedBots.filterNot { bot -> bot.botId == botId }
                    it.copy(
                        isSaving = false,
                        connectedBots = updatedList,
                        selectedBot = updatedList.firstOrNull(),
                        actionSuccessMessage = "Business bot disconnected"
                    )
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
