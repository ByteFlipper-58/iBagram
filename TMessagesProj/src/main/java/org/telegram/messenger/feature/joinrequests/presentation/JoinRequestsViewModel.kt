package org.telegram.messenger.feature.joinrequests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.joinrequests.domain.usecase.ApproveAllJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.ApproveJoinRequestUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.DismissAllJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.DismissJoinRequestUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.GetCachedJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.LoadJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.ObservePendingRequestsUseCase

class JoinRequestsViewModel(
    private val observePendingRequestsUseCase: ObservePendingRequestsUseCase,
    private val getCachedJoinRequestsUseCase: GetCachedJoinRequestsUseCase,
    private val loadJoinRequestsUseCase: LoadJoinRequestsUseCase,
    private val approveJoinRequestUseCase: ApproveJoinRequestUseCase,
    private val dismissJoinRequestUseCase: DismissJoinRequestUseCase,
    private val approveAllJoinRequestsUseCase: ApproveAllJoinRequestsUseCase,
    private val dismissAllJoinRequestsUseCase: DismissAllJoinRequestsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinRequestsUiState())
    val uiState: StateFlow<JoinRequestsUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var searchJob: Job? = null

    fun onEvent(event: JoinRequestsEvent) {
        when (event) {
            is JoinRequestsEvent.Load -> loadRequests(event.chatId)
            is JoinRequestsEvent.LoadMore -> loadMoreRequests()
            is JoinRequestsEvent.Search -> searchRequests(event.query)
            is JoinRequestsEvent.Approve -> approveRequest(event.userId)
            is JoinRequestsEvent.Dismiss -> dismissRequest(event.userId)
            is JoinRequestsEvent.ApproveAll -> approveAllRequests(event.inviteLink)
            is JoinRequestsEvent.DismissAll -> dismissAllRequests(event.inviteLink)
            is JoinRequestsEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadRequests(chatId: Long) {
        observationJob?.cancel()
        _uiState.update {
            it.copy(
                chatId = chatId,
                isLoading = true,
                errorMessage = null
            )
        }

        observationJob = viewModelScope.launch {
            observePendingRequestsUseCase(chatId).collect { pending ->
                _uiState.update { current ->
                    current.copy(pendingCount = pending.pendingCount)
                }
            }
        }

        viewModelScope.launch {
            val cached = getCachedJoinRequestsUseCase(chatId)
            if (!cached.isNullOrEmpty()) {
                _uiState.update { it.copy(requests = cached, isLoading = false) }
            }

            when (val result = loadJoinRequestsUseCase(chatId = chatId, query = null)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            requests = result.data.requests,
                            hasMore = result.data.hasMore,
                            isLoading = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreRequests() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.requests.isEmpty()) {
            return
        }

        val last = state.requests.lastOrNull() ?: return
        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val result = loadJoinRequestsUseCase(
                chatId = state.chatId,
                query = state.searchQuery.takeIf { it.isNotBlank() },
                offsetUserId = last.userId,
                offsetDate = last.date
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val combined = current.requests + result.data.requests
                        current.copy(
                            requests = combined,
                            hasMore = result.data.hasMore,
                            isLoadingMore = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMore = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun searchRequests(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        val chatId = _uiState.value.chatId

        _uiState.update { it.copy(searchQuery = trimmed, isLoading = true) }

        searchJob = viewModelScope.launch {
            when (val result = loadJoinRequestsUseCase(chatId = chatId, query = trimmed.takeIf { it.isNotBlank() })) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            requests = result.data.requests,
                            hasMore = result.data.hasMore,
                            isLoading = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun approveRequest(userId: Long) {
        val chatId = _uiState.value.chatId
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            when (val result = approveJoinRequestUseCase(chatId, userId)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedList = current.requests.filter { it.userId != userId }
                        current.copy(
                            requests = updatedList,
                            pendingCount = (current.pendingCount - 1).coerceAtLeast(0),
                            isProcessing = false,
                            actionSuccessMessage = "Request approved"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isProcessing = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun dismissRequest(userId: Long) {
        val chatId = _uiState.value.chatId
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            when (val result = dismissJoinRequestUseCase(chatId, userId)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedList = current.requests.filter { it.userId != userId }
                        current.copy(
                            requests = updatedList,
                            pendingCount = (current.pendingCount - 1).coerceAtLeast(0),
                            isProcessing = false,
                            actionSuccessMessage = "Request dismissed"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isProcessing = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun approveAllRequests(inviteLink: String?) {
        val chatId = _uiState.value.chatId
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            when (val result = approveAllJoinRequestsUseCase(chatId, inviteLink)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            requests = emptyList(),
                            pendingCount = 0,
                            hasMore = false,
                            isProcessing = false,
                            actionSuccessMessage = "All requests approved"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isProcessing = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun dismissAllRequests(inviteLink: String?) {
        val chatId = _uiState.value.chatId
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            when (val result = dismissAllJoinRequestsUseCase(chatId, inviteLink)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            requests = emptyList(),
                            pendingCount = 0,
                            hasMore = false,
                            isProcessing = false,
                            actionSuccessMessage = "All requests dismissed"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isProcessing = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
