package org.telegram.messenger.feature.business.billing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.business.billing.domain.usecase.GetBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ManageSubscriptionUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ObserveBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.QueryBillingPurchasesUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.StartBillingConnectionUseCase

class BillingViewModel(
    private val observeBillingStateUseCase: ObserveBillingStateUseCase,
    private val getBillingStateUseCase: GetBillingStateUseCase,
    private val startBillingConnectionUseCase: StartBillingConnectionUseCase,
    private val queryBillingPurchasesUseCase: QueryBillingPurchasesUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        val initialState = getBillingStateUseCase()
        _uiState.update {
            it.copy(
                isReady = initialState.isReady,
                isInvoiceMode = initialState.isInvoiceMode,
                premiumProduct = initialState.premiumProduct
            )
        }

        viewModelScope.launch {
            observeBillingStateUseCase().collect { state ->
                _uiState.update { current ->
                    current.copy(
                        isReady = state.isReady,
                        isInvoiceMode = state.isInvoiceMode,
                        premiumProduct = state.premiumProduct
                    )
                }
            }
        }
    }

    fun onEvent(event: BillingEvent) {
        when (event) {
            is BillingEvent.Connect -> connect()
            is BillingEvent.QueryPurchases -> queryPurchases(event.productType)
            is BillingEvent.ManageSubscription -> manageSubscription(event.productId)
            is BillingEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun connect() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = startBillingConnectionUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isReady = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun queryPurchases(productType: BillingProductType) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = queryBillingPurchasesUseCase(productType)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activePurchases = result.data
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun manageSubscription(productId: String = "telegram_premium") {
        viewModelScope.launch {
            when (val result = manageSubscriptionUseCase(productId)) {
                is Result.Success -> {
                    // Handled by launching external activity
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }
}
