package org.telegram.messenger.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.model.SearchFilter
import org.telegram.messenger.feature.search.domain.usecase.ClearRecentHashtagsUseCase
import org.telegram.messenger.feature.search.domain.usecase.ClearRecentSearchesUseCase
import org.telegram.messenger.feature.search.domain.usecase.GetRecentHashtagsUseCase
import org.telegram.messenger.feature.search.domain.usecase.GetRecentSearchesUseCase
import org.telegram.messenger.feature.search.domain.usecase.PutRecentHashtagUseCase
import org.telegram.messenger.feature.search.domain.usecase.RemoveRecentSearchUseCase
import org.telegram.messenger.feature.search.domain.usecase.SearchGlobalUseCase
import org.telegram.messenger.feature.search.domain.usecase.SearchLocalUseCase

class SearchViewModel(
    private val searchGlobalUseCase: SearchGlobalUseCase,
    private val searchLocalUseCase: SearchLocalUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val removeRecentSearchUseCase: RemoveRecentSearchUseCase,
    private val getRecentHashtagsUseCase: GetRecentHashtagsUseCase,
    private val putRecentHashtagUseCase: PutRecentHashtagUseCase,
    private val clearRecentHashtagsUseCase: ClearRecentHashtagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchEvent>()
    val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            val recentSearchesResult = getRecentSearchesUseCase()
            val recentHashtagsResult = getRecentHashtagsUseCase()

            val recentSearches = when (recentSearchesResult) {
                is Result.Success -> recentSearchesResult.data
                is Result.Failure -> emptyList()
            }

            val recentHashtags = when (recentHashtagsResult) {
                is Result.Success -> recentHashtagsResult.data
                is Result.Failure -> emptyList()
            }

            _uiState.value = _uiState.value.copy(
                recentSearches = recentSearches,
                recentHashtags = recentHashtags
            )
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                val newQuery = event.query
                _uiState.value = _uiState.value.copy(query = newQuery)
                if (newQuery.isBlank()) {
                    searchJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        localResults = emptyList(),
                        globalResults = emptyList()
                    )
                    loadRecents()
                } else {
                    performLocalSearch(newQuery)
                    scheduleGlobalSearch(newQuery, _uiState.value.filter)
                }
            }

            is SearchEvent.FilterChanged -> {
                _uiState.value = _uiState.value.copy(filter = event.filter)
                if (_uiState.value.query.isNotBlank()) {
                    performGlobalSearch(_uiState.value.query, event.filter)
                }
            }

            is SearchEvent.SearchSubmitted -> {
                searchJob?.cancel()
                val q = event.query
                _uiState.value = _uiState.value.copy(query = q)
                if (q.isNotBlank()) {
                    performLocalSearch(q)
                    performGlobalSearch(q, _uiState.value.filter)
                }
            }

            is SearchEvent.RecentSearchClicked -> {
                viewModelScope.launch {
                    _events.emit(event)
                }
            }

            is SearchEvent.ClearRecentSearches -> {
                viewModelScope.launch {
                    when (val result = clearRecentSearchesUseCase()) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(recentSearches = emptyList())
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is SearchEvent.RemoveRecentSearch -> {
                viewModelScope.launch {
                    when (val result = removeRecentSearchUseCase(event.id)) {
                        is Result.Success -> {
                            val updated = _uiState.value.recentSearches.filterNot { it.id == event.id }
                            _uiState.value = _uiState.value.copy(recentSearches = updated)
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is SearchEvent.HashtagClicked -> {
                viewModelScope.launch {
                    putRecentHashtagUseCase(event.hashtag)
                    onEvent(SearchEvent.QueryChanged(event.hashtag))
                }
            }

            is SearchEvent.ClearRecentHashtags -> {
                viewModelScope.launch {
                    when (val result = clearRecentHashtagsUseCase()) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(recentHashtags = emptyList())
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is SearchEvent.DismissError -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun performLocalSearch(query: String) {
        viewModelScope.launch {
            when (val result = searchLocalUseCase(query)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(localResults = result.data)
                }
                is Result.Failure -> {
                    // Local search failures are non-fatal, fallback to empty
                    _uiState.value = _uiState.value.copy(localResults = emptyList())
                }
            }
        }
    }

    private fun scheduleGlobalSearch(query: String, filter: SearchFilter) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performGlobalSearch(query, filter)
        }
    }

    private fun performGlobalSearch(query: String, filter: SearchFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = searchGlobalUseCase(query, filter)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        globalResults = result.data,
                        errorMessage = null
                    )
                }
                is Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        globalResults = emptyList(),
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }
}
