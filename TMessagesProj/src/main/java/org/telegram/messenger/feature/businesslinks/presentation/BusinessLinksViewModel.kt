package org.telegram.messenger.feature.businesslinks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.businesslinks.domain.usecase.CanAddNewBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.CreateBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.DeleteBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.EditBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.LoadBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.ObserveBusinessLinksUseCase

class BusinessLinksViewModel(
    private val observeBusinessLinksUseCase: ObserveBusinessLinksUseCase,
    private val loadBusinessLinksUseCase: LoadBusinessLinksUseCase,
    private val createBusinessLinkUseCase: CreateBusinessLinkUseCase,
    private val editBusinessLinkUseCase: EditBusinessLinkUseCase,
    private val deleteBusinessLinkUseCase: DeleteBusinessLinkUseCase,
    private val canAddNewBusinessLinkUseCase: CanAddNewBusinessLinkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessLinksUiState())
    val uiState: StateFlow<BusinessLinksUiState> = _uiState.asStateFlow()

    init {
        observeLinks()
        refreshCanAddNew()
    }

    private fun observeLinks() {
        viewModelScope.launch {
            observeBusinessLinksUseCase().collect { links ->
                _uiState.update { it.copy(links = links) }
                refreshCanAddNew()
            }
        }
    }

    fun onEvent(event: BusinessLinksEvent) {
        when (event) {
            is BusinessLinksEvent.Load -> load(event.forceReload)
            is BusinessLinksEvent.CreateLink -> createLink(event.input)
            is BusinessLinksEvent.EditLink -> editLink(event.slug, event.title, event.message)
            is BusinessLinksEvent.DeleteLink -> deleteLink(event.slug)
            is BusinessLinksEvent.RefreshCanAddNew -> refreshCanAddNew()
            is BusinessLinksEvent.ClearMessages -> clearMessages()
        }
    }

    private fun load(forceReload: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loadBusinessLinksUseCase(forceReload)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    refreshCanAddNew()
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun createLink(input: BusinessLinkInputModel?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = createBusinessLinkUseCase(input)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isProcessing = false, actionSuccessMessage = "Business link created: ${result.data.slug}")
                    }
                    refreshCanAddNew()
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun editLink(slug: String, title: String?, message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = editBusinessLinkUseCase(slug, title, message)) {
                is Result.Success -> _uiState.update {
                    it.copy(isProcessing = false, actionSuccessMessage = "Business link updated: ${result.data.slug}")
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun deleteLink(slug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = deleteBusinessLinkUseCase(slug)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isProcessing = false, actionSuccessMessage = "Business link deleted")
                    }
                    refreshCanAddNew()
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun refreshCanAddNew() {
        _uiState.update { it.copy(canAddNew = canAddNewBusinessLinkUseCase()) }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
