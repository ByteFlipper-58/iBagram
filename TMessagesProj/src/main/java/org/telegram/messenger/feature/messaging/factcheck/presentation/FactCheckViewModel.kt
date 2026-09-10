package org.telegram.messenger.feature.messaging.factcheck.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ApplyFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.DeleteFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckLimitUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.LoadFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ObserveFactCheckLoadedUseCase

class FactCheckViewModel(
    private val observeFactCheckLoadedUseCase: ObserveFactCheckLoadedUseCase,
    private val getFactCheckUseCase: GetFactCheckUseCase,
    private val loadFactCheckUseCase: LoadFactCheckUseCase,
    private val applyFactCheckUseCase: ApplyFactCheckUseCase,
    private val deleteFactCheckUseCase: DeleteFactCheckUseCase,
    private val getFactCheckLimitUseCase: GetFactCheckLimitUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FactCheckUiState())
    val uiState: StateFlow<FactCheckUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            val limit = getFactCheckLimitUseCase()
            _uiState.update { it.copy(characterLimit = limit) }
        }

        observeJob = viewModelScope.launch {
            observeFactCheckLoadedUseCase().collect {
                val state = _uiState.value
                if (state.dialogId != 0L && state.messageId != 0) {
                    val cached = getFactCheckUseCase(state.dialogId, state.messageId, state.factCheck?.hash ?: 0L)
                    if (cached != null) {
                        _uiState.update {
                            it.copy(
                                factCheck = cached,
                                currentInputText = cached.text,
                                currentEntities = cached.entities
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEvent(event: FactCheckEvent) {
        when (event) {
            is FactCheckEvent.LoadFactCheck -> loadFactCheck(event.dialogId, event.messageId, event.hash)
            is FactCheckEvent.UpdateInputText -> updateInputText(event.text)
            is FactCheckEvent.SetEntities -> setEntities(event.entities)
            is FactCheckEvent.ApplyFactCheck -> applyFactCheck()
            is FactCheckEvent.DeleteFactCheck -> deleteFactCheck()
            is FactCheckEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadFactCheck(dialogId: Long, messageId: Int, hash: Long) {
        _uiState.update {
            it.copy(
                dialogId = dialogId,
                messageId = messageId,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val cached = getFactCheckUseCase(dialogId, messageId, hash)
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        factCheck = cached,
                        currentInputText = cached.text,
                        currentEntities = cached.entities,
                        isLoading = false
                    )
                }
                return@launch
            }

            when (val result = loadFactCheckUseCase(dialogId, messageId)) {
                is Result.Success -> {
                    val model = result.data
                    _uiState.update {
                        it.copy(
                            factCheck = model,
                            currentInputText = model?.text.orEmpty(),
                            currentEntities = model?.entities ?: emptyList(),
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

    private fun updateInputText(text: String) {
        _uiState.update {
            it.copy(
                currentInputText = text,
                error = null
            )
        }
    }

    private fun setEntities(entities: List<FactCheckEntityModel>) {
        _uiState.update {
            it.copy(currentEntities = entities)
        }
    }

    private fun applyFactCheck() {
        val state = _uiState.value
        if (state.dialogId == 0L || state.messageId == 0) return
        if (state.isOverLimit) {
            _uiState.update { it.copy(error = "Fact check text exceeds character limit") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null, actionSuccessMessage = null) }

        viewModelScope.launch {
            val text = state.currentInputText.trim()
            val result = if (text.isEmpty()) {
                deleteFactCheckUseCase(state.dialogId, state.messageId)
            } else {
                applyFactCheckUseCase(state.dialogId, state.messageId, text, state.currentEntities.ifEmpty { null })
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            actionSuccessMessage = if (text.isEmpty()) "Fact check deleted" else "Fact check saved"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun deleteFactCheck() {
        val state = _uiState.value
        if (state.dialogId == 0L || state.messageId == 0) return

        _uiState.update { it.copy(isSubmitting = true, error = null, actionSuccessMessage = null) }

        viewModelScope.launch {
            when (val result = deleteFactCheckUseCase(state.dialogId, state.messageId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            factCheck = null,
                            currentInputText = "",
                            currentEntities = emptyList(),
                            actionSuccessMessage = "Fact check deleted"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                actionSuccessMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
