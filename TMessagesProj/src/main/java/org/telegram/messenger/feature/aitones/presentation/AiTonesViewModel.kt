package org.telegram.messenger.feature.aitones.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.aitones.domain.usecase.AddAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.EditAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.GetAiTonesStateUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.LoadAiTonesUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.ObserveAiTonesUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.RemoveAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.UnsaveAiToneUseCase

class AiTonesViewModel(
    private val observeAiTonesUseCase: ObserveAiTonesUseCase,
    private val getAiTonesStateUseCase: GetAiTonesStateUseCase,
    private val loadAiTonesUseCase: LoadAiTonesUseCase,
    private val addAiToneUseCase: AddAiToneUseCase,
    private val removeAiToneUseCase: RemoveAiToneUseCase,
    private val unsaveAiToneUseCase: UnsaveAiToneUseCase,
    private val editAiToneUseCase: EditAiToneUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiTonesUiState>(AiTonesUiState.Initial)
    val uiState: StateFlow<AiTonesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAiTonesUseCase().collect { stateModel ->
                val current = _uiState.value
                if (current is AiTonesUiState.Success) {
                    _uiState.value = current.copy(state = stateModel)
                } else {
                    _uiState.value = AiTonesUiState.Success(state = stateModel)
                }
            }
        }
    }

    fun onEvent(event: AiTonesEvent) {
        when (event) {
            is AiTonesEvent.LoadTones -> loadTones(force = false)
            is AiTonesEvent.RefreshTones -> loadTones(force = true)
            is AiTonesEvent.SelectTone -> selectTone(event.tone)
            is AiTonesEvent.AddTone -> addTone(event.tone)
            is AiTonesEvent.RemoveTone -> removeTone(event.tone)
            is AiTonesEvent.UnsaveTone -> unsaveTone(event.tone)
            is AiTonesEvent.EditTone -> editTone(event.tone)
            is AiTonesEvent.ClearError -> clearError()
        }
    }

    private fun loadTones(force: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is AiTonesUiState.Success) {
                _uiState.value = AiTonesUiState.Loading
            }
            when (val result = loadAiTonesUseCase(force)) {
                is Result.Success -> {
                    val currentSuccess = _uiState.value as? AiTonesUiState.Success
                    _uiState.value = currentSuccess?.copy(state = result.data)
                        ?: AiTonesUiState.Success(state = result.data)
                }
                is Result.Failure -> {
                    _uiState.value = AiTonesUiState.Error(result.error.message)
                }
            }
        }
    }

    private fun selectTone(tone: AiToneModel?) {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        _uiState.value = current.copy(selectedTone = tone)
    }

    private fun addTone(tone: AiToneModel) {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)
            when (val result = addAiToneUseCase(tone)) {
                is Result.Success -> {
                    _uiState.value = current.copy(isSaving = false)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun removeTone(tone: AiToneModel) {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)
            when (val result = removeAiToneUseCase(tone)) {
                is Result.Success -> {
                    _uiState.value = current.copy(isSaving = false)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun unsaveTone(tone: AiToneModel) {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)
            when (val result = unsaveAiToneUseCase(tone)) {
                is Result.Success -> {
                    _uiState.value = current.copy(isSaving = false)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun editTone(tone: AiToneModel) {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)
            when (val result = editAiToneUseCase(tone)) {
                is Result.Success -> {
                    _uiState.value = current.copy(isSaving = false)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun clearError() {
        val current = _uiState.value as? AiTonesUiState.Success ?: return
        _uiState.value = current.copy(errorMessage = null)
    }
}
