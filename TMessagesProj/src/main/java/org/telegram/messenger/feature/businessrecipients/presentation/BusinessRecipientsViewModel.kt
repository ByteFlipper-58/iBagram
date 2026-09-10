package org.telegram.messenger.feature.businessrecipients.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientValidationResult
import org.telegram.messenger.feature.businessrecipients.domain.usecase.AddExcludedUsersUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.AddSelectedUsersUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.CheckRecipientsChangesUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.GetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ObserveBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.RemoveExcludedUserUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.RemoveSelectedUserUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ResetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.SetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ToggleExcludeSelectedUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ToggleRecipientFilterUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ValidateBusinessRecipientsUseCase

class BusinessRecipientsViewModel(
    private val observeRecipientsUseCase: ObserveBusinessRecipientsUseCase,
    private val getRecipientsUseCase: GetBusinessRecipientsUseCase,
    private val setRecipientsUseCase: SetBusinessRecipientsUseCase,
    private val toggleExcludeSelectedUseCase: ToggleExcludeSelectedUseCase,
    private val toggleRecipientFilterUseCase: ToggleRecipientFilterUseCase,
    private val addSelectedUsersUseCase: AddSelectedUsersUseCase,
    private val removeSelectedUserUseCase: RemoveSelectedUserUseCase,
    private val addExcludedUsersUseCase: AddExcludedUsersUseCase,
    private val removeExcludedUserUseCase: RemoveExcludedUserUseCase,
    private val checkChangesUseCase: CheckRecipientsChangesUseCase,
    private val validateUseCase: ValidateBusinessRecipientsUseCase,
    private val resetUseCase: ResetBusinessRecipientsUseCase
) : ViewModel() {

    private val initialSnapshot = getRecipientsUseCase()

    private val _uiState = MutableStateFlow(
        BusinessRecipientsUiState(
            initialModel = initialSnapshot,
            currentModel = initialSnapshot,
            hasChanges = false,
            validationResult = validateUseCase(initialSnapshot)
        )
    )
    val uiState: StateFlow<BusinessRecipientsUiState> = _uiState.asStateFlow()

    init {
        observeRecipientsUseCase()
            .onEach { current ->
                _uiState.update { state ->
                    val changes = checkChangesUseCase(state.initialModel, current)
                    val validation = validateUseCase(current)
                    state.copy(
                        currentModel = current,
                        hasChanges = changes,
                        validationResult = validation
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BusinessRecipientsEvent) {
        when (event) {
            is BusinessRecipientsEvent.SetInitial -> {
                setRecipientsUseCase(event.model)
                _uiState.update {
                    it.copy(
                        initialModel = event.model,
                        currentModel = event.model,
                        hasChanges = false,
                        validationResult = validateUseCase(event.model)
                    )
                }
            }
            is BusinessRecipientsEvent.ToggleExclude -> toggleExcludeSelectedUseCase(event.exclude)
            is BusinessRecipientsEvent.ToggleFilter -> toggleRecipientFilterUseCase(event.filter, event.enabled)
            is BusinessRecipientsEvent.AddSelectedUsers -> addSelectedUsersUseCase(event.userIds)
            is BusinessRecipientsEvent.RemoveSelectedUser -> removeSelectedUserUseCase(event.userId)
            is BusinessRecipientsEvent.AddExcludedUsers -> addExcludedUsersUseCase(event.userIds)
            is BusinessRecipientsEvent.RemoveExcludedUser -> removeExcludedUserUseCase(event.userId)
            BusinessRecipientsEvent.Validate -> {
                val validation = validateUseCase(_uiState.value.currentModel)
                _uiState.update { it.copy(validationResult = validation) }
            }
            BusinessRecipientsEvent.Reset -> resetUseCase()
        }
    }

    fun validate(): RecipientValidationResult {
        val result = validateUseCase(_uiState.value.currentModel)
        _uiState.update { it.copy(validationResult = result) }
        return result
    }
}
