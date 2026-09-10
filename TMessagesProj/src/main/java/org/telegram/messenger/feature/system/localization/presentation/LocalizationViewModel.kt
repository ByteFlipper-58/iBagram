package org.telegram.messenger.feature.system.localization.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.repository.LocalizationRepository
import org.telegram.messenger.feature.system.localization.domain.usecase.ApplyLocaleUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.ObserveLocalizationStateUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.SetNameDisplayOrderUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.Toggle24HourFormatUseCase

class LocalizationViewModel(
    private val observeLocalizationStateUseCase: ObserveLocalizationStateUseCase,
    private val applyLocaleUseCase: ApplyLocaleUseCase,
    private val toggle24HourFormatUseCase: Toggle24HourFormatUseCase,
    private val setNameDisplayOrderUseCase: SetNameDisplayOrderUseCase,
    private val repository: LocalizationRepository,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope = scope ?: CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val _uiState = MutableStateFlow(LocalizationUiState())
    val uiState: StateFlow<LocalizationUiState> = _uiState.asStateFlow()

    init {
        observeLocalizationStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    val filtered = filterLocales(state.availableLocales, current.searchQuery)
                    current.copy(
                        currentLocale = state.currentLocale,
                        availableLocales = state.availableLocales,
                        is24HourFormat = state.is24HourFormat,
                        nameDisplayOrder = state.nameDisplayOrder,
                        isRtl = state.isRtl,
                        customStringsCount = state.customStringOverrides.size,
                        filteredLocales = filtered
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    private fun filterLocales(locales: List<LocaleModel>, query: String): List<LocaleModel> {
        if (query.isBlank()) return locales
        val lower = query.lowercase().trim()
        return locales.filter {
            it.nameOrEnglish().lowercase().contains(lower) ||
            it.nativeName.lowercase().contains(lower) ||
            it.code.lowercase().contains(lower)
        }
    }

    private fun LocaleModel.nameOrEnglish(): String = englishName.ifEmpty { nativeName }

    fun onEvent(event: LocalizationEvent) {
        when (event) {
            is LocalizationEvent.SelectLocale -> {
                coroutineScope.launch {
                    applyLocaleUseCase(event.locale)
                }
            }
            is LocalizationEvent.SearchLocales -> {
                _uiState.update { current ->
                    val filtered = filterLocales(current.availableLocales, event.query)
                    current.copy(searchQuery = event.query, filteredLocales = filtered)
                }
            }
            is LocalizationEvent.Toggle24HourFormat -> {
                coroutineScope.launch {
                    toggle24HourFormatUseCase(event.enabled)
                }
            }
            is LocalizationEvent.ChangeNameDisplayOrder -> {
                coroutineScope.launch {
                    setNameDisplayOrderUseCase(event.order)
                }
            }
            is LocalizationEvent.ApplyCustomStrings -> {
                coroutineScope.launch {
                    repository.setCustomStrings(event.strings)
                }
            }
            is LocalizationEvent.ResetCustomStrings -> {
                coroutineScope.launch {
                    repository.clearCustomStrings()
                }
            }
        }
    }
}
