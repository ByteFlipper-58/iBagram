package org.telegram.messenger.feature.messaging.translate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.usecase.AddDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ApplyAppLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetAvailableLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.RemoveDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetChatTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetContextTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDialogTargetLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDoNotTranslateLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ToggleDialogTranslatingUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.TranslateTextUseCase

class TranslateViewModel(
    private val observeTranslateSettingsUseCase: ObserveTranslateSettingsUseCase,
    private val getTranslateSettingsUseCase: GetTranslateSettingsUseCase,
    private val setChatTranslateEnabledUseCase: SetChatTranslateEnabledUseCase,
    private val setContextTranslateEnabledUseCase: SetContextTranslateEnabledUseCase,
    private val setDoNotTranslateLanguagesUseCase: SetDoNotTranslateLanguagesUseCase,
    private val addDoNotTranslateLanguageUseCase: AddDoNotTranslateLanguageUseCase,
    private val removeDoNotTranslateLanguageUseCase: RemoveDoNotTranslateLanguageUseCase,
    private val observeDialogTranslationStateUseCase: ObserveDialogTranslationStateUseCase,
    private val getDialogTranslationStateUseCase: GetDialogTranslationStateUseCase,
    private val toggleDialogTranslatingUseCase: ToggleDialogTranslatingUseCase,
    private val setDialogTargetLanguageUseCase: SetDialogTargetLanguageUseCase,
    private val translateTextUseCase: TranslateTextUseCase,
    private val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase,
    private val applyAppLanguageUseCase: ApplyAppLanguageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private var dialogObservationJob: Job? = null

    init {
        observeTranslateSettingsUseCase()
            .onEach { settings ->
                _uiState.update { current ->
                    current.copy(settings = settings)
                }
            }
            .launchIn(viewModelScope)

        loadAvailableLanguages()
    }

    fun onEvent(event: TranslateEvent) {
        when (event) {
            is TranslateEvent.LoadSettings -> loadSettings()
            is TranslateEvent.LoadAvailableLanguages -> loadAvailableLanguages()
            is TranslateEvent.LoadDialogState -> observeDialog(event.dialogId)
            is TranslateEvent.SetChatTranslateEnabled -> setChatTranslateEnabled(event.enabled)
            is TranslateEvent.SetContextTranslateEnabled -> setContextTranslateEnabled(event.enabled)
            is TranslateEvent.AddDoNotTranslateLanguage -> addDoNotTranslateLanguage(event.languageCode)
            is TranslateEvent.RemoveDoNotTranslateLanguage -> removeDoNotTranslateLanguage(event.languageCode)
            is TranslateEvent.SetDoNotTranslateLanguages -> setDoNotTranslateLanguages(event.languages)
            is TranslateEvent.ToggleDialogTranslating -> toggleDialogTranslating(event.dialogId, event.enabled)
            is TranslateEvent.SetDialogTargetLanguage -> setDialogTargetLanguage(event.dialogId, event.languageCode)
            is TranslateEvent.TranslateText -> translateText(event.text, event.fromLanguage, event.toLanguage)
            is TranslateEvent.ApplyAppLanguage -> applyAppLanguage(event.languageCode)
            is TranslateEvent.ClearMessages -> clearMessages()
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            when (val result = getTranslateSettingsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(settings = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun loadAvailableLanguages() {
        viewModelScope.launch {
            when (val result = getAvailableLanguagesUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(availableLanguages = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun observeDialog(dialogId: Long) {
        dialogObservationJob?.cancel()
        dialogObservationJob = observeDialogTranslationStateUseCase(dialogId)
            .onEach { dialogState ->
                _uiState.update { current ->
                    current.copy(currentDialogState = dialogState)
                }
            }
            .launchIn(viewModelScope)
    }

    fun setChatTranslateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = setChatTranslateEnabledUseCase(enabled)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            settings = current.settings.copy(isChatTranslateEnabled = enabled),
                            actionSuccessMessage = "Chat translate updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun setContextTranslateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = setContextTranslateEnabledUseCase(enabled)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            settings = current.settings.copy(isContextTranslateEnabled = enabled),
                            actionSuccessMessage = "Context translate updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun addDoNotTranslateLanguage(languageCode: String) {
        viewModelScope.launch {
            when (val result = addDoNotTranslateLanguageUseCase(languageCode)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updated = current.settings.doNotTranslateLanguages + languageCode
                        current.copy(
                            settings = current.settings.copy(doNotTranslateLanguages = updated),
                            actionSuccessMessage = "Language added to exceptions"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun removeDoNotTranslateLanguage(languageCode: String) {
        viewModelScope.launch {
            when (val result = removeDoNotTranslateLanguageUseCase(languageCode)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updated = current.settings.doNotTranslateLanguages - languageCode
                        current.copy(
                            settings = current.settings.copy(doNotTranslateLanguages = updated),
                            actionSuccessMessage = "Language removed from exceptions"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun setDoNotTranslateLanguages(languages: Set<String>) {
        viewModelScope.launch {
            when (val result = setDoNotTranslateLanguagesUseCase(languages)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            settings = current.settings.copy(doNotTranslateLanguages = languages),
                            actionSuccessMessage = "Exception languages updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun toggleDialogTranslating(dialogId: Long, enabled: Boolean) {
        viewModelScope.launch {
            when (val result = toggleDialogTranslatingUseCase(dialogId, enabled)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedDialog = current.currentDialogState?.copy(isTranslating = enabled)
                        current.copy(
                            currentDialogState = updatedDialog,
                            actionSuccessMessage = if (enabled) "Translation enabled" else "Translation disabled"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun setDialogTargetLanguage(dialogId: Long, languageCode: String) {
        viewModelScope.launch {
            when (val result = setDialogTargetLanguageUseCase(dialogId, languageCode)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedDialog = current.currentDialogState?.copy(targetLanguage = languageCode)
                        current.copy(
                            currentDialogState = updatedDialog,
                            actionSuccessMessage = "Target language updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun translateText(text: String, fromLanguage: String?, toLanguage: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = translateTextUseCase(text, fromLanguage, toLanguage)) {
                is Result.Success -> {
                    _uiState.update { it.copy(lastTranslation = result.data, isLoading = false) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun applyAppLanguage(languageCode: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = applyAppLanguageUseCase(languageCode)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        val updatedLangs = current.availableLanguages.map {
                            it.copy(isCurrent = it.code == languageCode)
                        }
                        current.copy(
                            availableLanguages = updatedLangs,
                            isLoading = false,
                            actionSuccessMessage = "App language applied"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
