package org.telegram.messenger.feature.business.payments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTopupOptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarsBalanceUseCase

class PaymentsViewModel(
    private val observeStarsBalanceUseCase: ObserveStarsBalanceUseCase,
    private val observeStarTransactionsUseCase: ObserveStarTransactionsUseCase,
    private val observeStarSubscriptionsUseCase: ObserveStarSubscriptionsUseCase,
    private val getStarTopupOptionsUseCase: GetStarTopupOptionsUseCase,
    private val refreshStarsBalanceUseCase: RefreshStarsBalanceUseCase,
    private val refreshStarTransactionsUseCase: RefreshStarTransactionsUseCase,
    private val refreshStarSubscriptionsUseCase: RefreshStarSubscriptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState(isLoading = true))
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaymentsEvent>()
    val events: SharedFlow<PaymentsEvent> = _events.asSharedFlow()

    init {
        observeBalance()
        observeTransactions()
        observeSubscriptions()
        loadTopupOptions()
    }

    private fun observeBalance() {
        viewModelScope.launch {
            observeStarsBalanceUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { balance ->
                    _uiState.update { it.copy(balance = balance, isLoading = false) }
                }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            observeStarTransactionsUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { txs ->
                    _uiState.update { it.copy(transactions = txs) }
                }
        }
    }

    private fun observeSubscriptions() {
        viewModelScope.launch {
            observeStarSubscriptionsUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { subs ->
                    _uiState.update { it.copy(subscriptions = subs) }
                }
        }
    }

    fun loadTopupOptions() {
        viewModelScope.launch {
            when (val result = getStarTopupOptionsUseCase()) {
                is Result.Success -> _uiState.update { it.copy(topupOptions = result.data) }
                is Result.Failure -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun onEvent(event: PaymentsEvent) {
        viewModelScope.launch {
            _events.emit(event)
            when (event) {
                is PaymentsEvent.RefreshAll -> {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    refreshStarsBalanceUseCase()
                    refreshStarTransactionsUseCase()
                    refreshStarSubscriptionsUseCase()
                    loadTopupOptions()
                    _uiState.update { it.copy(isLoading = false) }
                }
                is PaymentsEvent.RefreshBalance -> {
                    refreshStarsBalanceUseCase()
                }
                is PaymentsEvent.RefreshTransactions -> {
                    refreshStarTransactionsUseCase()
                }
                is PaymentsEvent.RefreshSubscriptions -> {
                    refreshStarSubscriptionsUseCase()
                }
            }
        }
    }
}
