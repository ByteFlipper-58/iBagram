package org.telegram.messenger.feature.hashtagsearch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.AddHashtagToHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ClearHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ClearHashtagSearchResultsUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.GetHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.JumpToHashtagMessageUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ObserveHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ObserveHashtagSearchResultUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.RemoveHashtagFromHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.SearchHashtagUseCase

class HashtagSearchViewModel(
    val observeHashtagHistoryUseCase: ObserveHashtagHistoryUseCase,
    val getHashtagHistoryUseCase: GetHashtagHistoryUseCase,
    val addHashtagToHistoryUseCase: AddHashtagToHistoryUseCase,
    val removeHashtagFromHistoryUseCase: RemoveHashtagFromHistoryUseCase,
    val clearHashtagHistoryUseCase: ClearHashtagHistoryUseCase,
    val observeHashtagSearchResultUseCase: ObserveHashtagSearchResultUseCase,
    val searchHashtagUseCase: SearchHashtagUseCase,
    val jumpToHashtagMessageUseCase: JumpToHashtagMessageUseCase,
    val clearHashtagSearchResultsUseCase: ClearHashtagSearchResultsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HashtagSearchUiState(history = getHashtagHistoryUseCase())
    )
    val uiState: StateFlow<HashtagSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeHashtagHistoryUseCase().collect { historyList ->
                _uiState.update { it.copy(history = historyList) }
            }
        }
    }

    fun onEvent(event: HashtagSearchEvent) {
        when (event) {
            is HashtagSearchEvent.Search -> search(
                query = event.query,
                searchType = event.searchType,
                guid = event.guid
            )
            is HashtagSearchEvent.SetSearchType -> setSearchType(event.searchType)
            is HashtagSearchEvent.JumpToMessage -> jumpToMessage(event.index, event.guid)
            is HashtagSearchEvent.AddToHistory -> addToHistory(event.hashtag)
            is HashtagSearchEvent.RemoveFromHistory -> removeFromHistory(event.hashtag)
            is HashtagSearchEvent.ClearHistory -> clearHistory()
            is HashtagSearchEvent.ClearResults -> clearResults()
            is HashtagSearchEvent.ClearError -> clearError()
        }
    }

    private fun search(query: String, searchType: HashtagSearchType, guid: Int) {
        if (query.isBlank()) return
        _uiState.update {
            it.copy(
                query = query,
                searchType = searchType,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = searchHashtagUseCase(query, searchType, guid)) {
                is Result.Success -> {
                    addHashtagToHistoryUseCase(query)
                    _uiState.update {
                        it.copy(
                            searchResult = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message ?: "Failed to search hashtag"
                        )
                    }
                }
            }
        }
    }

    private fun setSearchType(searchType: HashtagSearchType) {
        val currentQuery = _uiState.value.query
        _uiState.update { it.copy(searchType = searchType) }
        if (currentQuery.isNotBlank()) {
            search(currentQuery, searchType, guid = 0)
        }
    }

    private fun jumpToMessage(index: Int, guid: Int) {
        val currentType = _uiState.value.searchType
        viewModelScope.launch {
            when (val result = jumpToHashtagMessageUseCase(guid, index, currentType)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            searchResult = state.searchResult?.copy(selectedIndex = index)
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    private fun addToHistory(hashtag: String) {
        viewModelScope.launch {
            addHashtagToHistoryUseCase(hashtag)
        }
    }

    private fun removeFromHistory(hashtag: String) {
        viewModelScope.launch {
            removeHashtagFromHistoryUseCase(hashtag)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            clearHashtagHistoryUseCase()
        }
    }

    private fun clearResults() {
        clearHashtagSearchResultsUseCase(_uiState.value.searchType)
        _uiState.update {
            it.copy(
                searchResult = null,
                query = "",
                isLoading = false
            )
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
