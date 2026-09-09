package org.telegram.messenger.feature.stargifts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.stargifts.domain.usecase.GetStarGiftByIdUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.GetStarGiftsCatalogUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.LoadProfileGiftsUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ObserveProfileGiftsUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ObserveStarGiftsCatalogUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ToggleHideProfileGiftUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.TogglePinProfileGiftUseCase

class StarGiftsViewModel(
    private val observeStarGiftsCatalogUseCase: ObserveStarGiftsCatalogUseCase,
    private val getStarGiftsCatalogUseCase: GetStarGiftsCatalogUseCase,
    private val getStarGiftByIdUseCase: GetStarGiftByIdUseCase,
    private val observeProfileGiftsUseCase: ObserveProfileGiftsUseCase,
    private val loadProfileGiftsUseCase: LoadProfileGiftsUseCase,
    private val togglePinProfileGiftUseCase: TogglePinProfileGiftUseCase,
    private val toggleHideProfileGiftUseCase: ToggleHideProfileGiftUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StarGiftsUiState>(StarGiftsUiState.Initial)
    val uiState: StateFlow<StarGiftsUiState> = _uiState.asStateFlow()

    private var catalogObserveJob: Job? = null
    private var profileObserveJob: Job? = null

    init {
        loadCatalog()
    }

    fun onEvent(event: StarGiftsEvent) {
        when (event) {
            is StarGiftsEvent.LoadCatalog -> loadCatalog(event.forceRefresh)
            is StarGiftsEvent.LoadProfileGifts -> loadProfileGifts(event.dialogId, event.offset)
            is StarGiftsEvent.SelectGift -> selectGift(event.giftId)
            is StarGiftsEvent.TogglePin -> togglePin(event.dialogId, event.giftId, event.pin)
            is StarGiftsEvent.ToggleHide -> toggleHide(event.dialogId, event.giftId, event.hide)
            is StarGiftsEvent.ApplyFilter -> applyFilter(event.dialogId, event.filter)
            is StarGiftsEvent.ClearError -> clearError()
        }
    }

    fun loadCatalog(forceRefresh: Boolean = false) {
        catalogObserveJob?.cancel()
        if (_uiState.value is StarGiftsUiState.Initial) {
            _uiState.value = StarGiftsUiState.Loading
        }

        catalogObserveJob = viewModelScope.launch {
            observeStarGiftsCatalogUseCase()
                .catch { e ->
                    _uiState.value = StarGiftsUiState.Error(e.message ?: "Failed to observe star gifts")
                }
                .collect { catalogState ->
                    val currentState = _uiState.value
                    if (currentState is StarGiftsUiState.Success) {
                        _uiState.value = currentState.copy(catalog = catalogState.gifts)
                    } else {
                        _uiState.value = StarGiftsUiState.Success(catalog = catalogState.gifts)
                    }
                }
        }
    }

    fun loadProfileGifts(dialogId: Long, offset: String? = null) {
        profileObserveJob?.cancel()
        val current = _uiState.value as? StarGiftsUiState.Success ?: StarGiftsUiState.Success()
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = loadProfileGiftsUseCase(dialogId, offset, 30, current.filter)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(
                        profileGifts = result.data,
                        isProcessing = false
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun selectGift(giftId: Long) {
        val current = _uiState.value as? StarGiftsUiState.Success ?: return
        viewModelScope.launch {
            when (val res = getStarGiftByIdUseCase(giftId)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(selectedGift = res.data)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(errorMessage = res.error.message)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun togglePin(dialogId: Long, giftId: Long, pin: Boolean) {
        val current = _uiState.value as? StarGiftsUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val res = togglePinProfileGiftUseCase(dialogId, giftId, pin)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = res.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun toggleHide(dialogId: Long, giftId: Long, hide: Boolean) {
        val current = _uiState.value as? StarGiftsUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val res = toggleHideProfileGiftUseCase(dialogId, giftId, hide)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? StarGiftsUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = res.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun applyFilter(dialogId: Long, filter: StarGiftFilter) {
        val current = _uiState.value as? StarGiftsUiState.Success ?: return
        _uiState.value = current.copy(filter = filter)
        loadProfileGifts(dialogId, null)
    }

    fun clearError() {
        val current = _uiState.value as? StarGiftsUiState.Success ?: return
        _uiState.value = current.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        catalogObserveJob?.cancel()
        profileObserveJob?.cancel()
    }
}
