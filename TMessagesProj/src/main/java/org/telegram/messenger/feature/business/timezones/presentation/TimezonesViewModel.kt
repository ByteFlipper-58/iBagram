package org.telegram.messenger.feature.business.timezones.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.business.timezones.domain.usecase.FindTimezoneUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetSystemTimezoneIdUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezoneNameUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.LoadTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.ObserveTimezonesUseCase

class TimezonesViewModel(
    private val observeTimezonesUseCase: ObserveTimezonesUseCase,
    private val getTimezonesUseCase: GetTimezonesUseCase,
    private val loadTimezonesUseCase: LoadTimezonesUseCase,
    private val findTimezoneUseCase: FindTimezoneUseCase,
    private val getSystemTimezoneIdUseCase: GetSystemTimezoneIdUseCase,
    private val getTimezoneNameUseCase: GetTimezoneNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimezonesUiState())
    val uiState: StateFlow<TimezonesUiState> = _uiState.asStateFlow()

    init {
        val systemId = getSystemTimezoneIdUseCase()
        _uiState.update { it.copy(systemTimezoneId = systemId) }

        viewModelScope.launch {
            observeTimezonesUseCase().collect { timezones ->
                _uiState.update { current ->
                    current.copy(
                        timezones = timezones,
                        filteredTimezones = filterTimezones(timezones, current.query)
                    )
                }
            }
        }
    }

    fun onEvent(event: TimezonesEvent) {
        when (event) {
            is TimezonesEvent.Load -> load(event.forceReload)
            is TimezonesEvent.Search -> search(event.query)
            is TimezonesEvent.SelectTimezone -> selectTimezone(event.timezoneId)
            is TimezonesEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun load(forceReload: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = loadTimezonesUseCase(forceReload)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            timezones = result.data,
                            filteredTimezones = filterTimezones(result.data, current.query)
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

    fun search(query: String) {
        _uiState.update { current ->
            current.copy(
                query = query,
                filteredTimezones = filterTimezones(current.timezones, query)
            )
        }
    }

    fun selectTimezone(timezoneId: String) {
        _uiState.update { it.copy(selectedTimezoneId = timezoneId) }
    }

    fun getTimezoneName(id: String, withOffset: Boolean = false): String {
        return getTimezoneNameUseCase(id, withOffset)
    }

    fun findTimezone(id: String): TimezoneModel? {
        return findTimezoneUseCase(id)
    }

    private fun filterTimezones(timezones: List<TimezoneModel>, query: String): List<TimezoneModel> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return timezones.filter {
            it.name.lowercase().contains(q) ||
            it.id.lowercase().contains(q) ||
            it.formattedOffset.lowercase().contains(q)
        }
    }
}
