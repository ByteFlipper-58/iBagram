package org.telegram.messenger.feature.birthdays.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.birthdays.domain.usecase.CheckBirthdaysUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.GetBirthdaysStateUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.HasBirthdaysTodayUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.HideTodayBirthdaysUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.IsBirthdayTodayUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.ObserveBirthdaysUseCase

class BirthdaysViewModel(
    private val observeBirthdaysUseCase: ObserveBirthdaysUseCase,
    private val getBirthdaysStateUseCase: GetBirthdaysStateUseCase,
    private val checkBirthdaysUseCase: CheckBirthdaysUseCase,
    private val hideTodayBirthdaysUseCase: HideTodayBirthdaysUseCase,
    private val isBirthdayTodayUseCase: IsBirthdayTodayUseCase,
    private val hasBirthdaysTodayUseCase: HasBirthdaysTodayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BirthdaysUiState())
    val uiState: StateFlow<BirthdaysUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeJob = viewModelScope.launch {
            observeBirthdaysUseCase().collect { birthdayState ->
                val hasToday = birthdayState != null && !birthdayState.isTodayEmpty
                _uiState.update {
                    it.copy(
                        state = birthdayState,
                        isBannerVisible = hasToday
                    )
                }
            }
        }
    }

    fun onEvent(event: BirthdaysEvent) {
        when (event) {
            is BirthdaysEvent.CheckBirthdays -> checkBirthdays(event.force)
            is BirthdaysEvent.DismissTodayBanner -> dismissTodayBanner()
            is BirthdaysEvent.ClearError -> clearError()
        }
    }

    private fun checkBirthdays(force: Boolean) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = checkBirthdaysUseCase(force)) {
                is Result.Success -> {
                    val birthdayState = result.data
                    val hasToday = birthdayState != null && !birthdayState.isTodayEmpty
                    _uiState.update {
                        it.copy(
                            state = birthdayState,
                            isBannerVisible = hasToday,
                            isLoading = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun dismissTodayBanner() {
        _uiState.update { it.copy(isBannerVisible = false) }
        viewModelScope.launch {
            hideTodayBirthdaysUseCase()
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun isBirthdayToday(userId: Long): Boolean {
        return isBirthdayTodayUseCase(userId)
    }

    suspend fun hasBirthdaysToday(): Boolean {
        return hasBirthdaysTodayUseCase()
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
