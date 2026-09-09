package org.telegram.messenger.feature.giftauctions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetAuctionByIdUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetAuctionBySlugUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.LoadAuctionAcquiredGiftsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.ObserveActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.ObserveAuctionUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.RefreshActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.SendAuctionBidUseCase

class GiftAuctionsViewModel(
    private val observeActiveAuctionsUseCase: ObserveActiveAuctionsUseCase,
    private val observeAuctionUseCase: ObserveAuctionUseCase,
    private val getActiveAuctionsUseCase: GetActiveAuctionsUseCase,
    private val getAuctionByIdUseCase: GetAuctionByIdUseCase,
    private val getAuctionBySlugUseCase: GetAuctionBySlugUseCase,
    private val sendAuctionBidUseCase: SendAuctionBidUseCase,
    private val loadAuctionAcquiredGiftsUseCase: LoadAuctionAcquiredGiftsUseCase,
    private val refreshActiveAuctionsUseCase: RefreshActiveAuctionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GiftAuctionsUiState>(GiftAuctionsUiState.Initial)
    val uiState: StateFlow<GiftAuctionsUiState> = _uiState.asStateFlow()

    private var selectedAuctionJob: Job? = null

    init {
        viewModelScope.launch {
            observeActiveAuctionsUseCase().collect { auctions ->
                val current = _uiState.value
                if (current is GiftAuctionsUiState.Success) {
                    _uiState.value = current.copy(activeAuctions = auctions)
                } else {
                    _uiState.value = GiftAuctionsUiState.Success(activeAuctions = auctions)
                }
            }
        }
    }

    fun onEvent(event: GiftAuctionsEvent) {
        when (event) {
            is GiftAuctionsEvent.LoadActiveAuctions -> loadActiveAuctions()
            is GiftAuctionsEvent.RefreshActiveAuctions -> refreshActiveAuctions()
            is GiftAuctionsEvent.SelectAuction -> selectAuction(event.giftId)
            is GiftAuctionsEvent.LoadAuctionBySlug -> loadAuctionBySlug(event.slug)
            is GiftAuctionsEvent.SendBid -> sendBid(event.giftId, event.amount, event.params)
            is GiftAuctionsEvent.LoadAcquiredGifts -> loadAcquiredGifts(event.giftId)
            is GiftAuctionsEvent.ClearError -> clearError()
        }
    }

    private fun loadActiveAuctions() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is GiftAuctionsUiState.Success) {
                _uiState.value = GiftAuctionsUiState.Loading
            }
            val auctions = getActiveAuctionsUseCase()
            val currentSuccess = _uiState.value as? GiftAuctionsUiState.Success
            _uiState.value = currentSuccess?.copy(activeAuctions = auctions)
                ?: GiftAuctionsUiState.Success(activeAuctions = auctions)
        }
    }

    private fun refreshActiveAuctions() {
        val current = _uiState.value as? GiftAuctionsUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isRefreshing = true)
            when (val result = refreshActiveAuctionsUseCase()) {
                is Result.Success -> {
                    val auctions = getActiveAuctionsUseCase()
                    _uiState.value = current.copy(isRefreshing = false, activeAuctions = auctions)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isRefreshing = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun selectAuction(giftId: Long) {
        selectedAuctionJob?.cancel()
        selectedAuctionJob = viewModelScope.launch {
            observeAuctionUseCase(giftId).collect { auction ->
                val current = _uiState.value as? GiftAuctionsUiState.Success
                if (current != null) {
                    _uiState.value = current.copy(selectedAuction = auction)
                }
            }
        }
    }

    private fun loadAuctionBySlug(slug: String) {
        viewModelScope.launch {
            when (val result = getAuctionBySlugUseCase(slug)) {
                is Result.Success -> {
                    val current = _uiState.value as? GiftAuctionsUiState.Success
                    if (current != null) {
                        _uiState.value = current.copy(selectedAuction = result.data)
                    } else {
                        _uiState.value = GiftAuctionsUiState.Success(selectedAuction = result.data)
                    }
                    selectAuction(result.data.giftId)
                }
                is Result.Failure -> {
                    val current = _uiState.value as? GiftAuctionsUiState.Success
                    if (current != null) {
                        _uiState.value = current.copy(errorMessage = result.error.message)
                    } else {
                        _uiState.value = GiftAuctionsUiState.Error(result.error.message)
                    }
                }
            }
        }
    }

    private fun sendBid(giftId: Long, amount: Long, params: GiftAuctionBidParamsModel?) {
        val current = _uiState.value as? GiftAuctionsUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isBidding = true)
            when (val result = sendAuctionBidUseCase(giftId, amount, params)) {
                is Result.Success -> {
                    _uiState.value = current.copy(isBidding = false)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isBidding = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun loadAcquiredGifts(giftId: Long) {
        val current = _uiState.value as? GiftAuctionsUiState.Success ?: return
        viewModelScope.launch {
            when (val result = loadAuctionAcquiredGiftsUseCase(giftId)) {
                is Result.Success -> {
                    _uiState.value = current.copy(acquiredGifts = result.data)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(errorMessage = result.error.message)
                }
            }
        }
    }

    private fun clearError() {
        val current = _uiState.value as? GiftAuctionsUiState.Success ?: return
        _uiState.value = current.copy(errorMessage = null)
    }
}
