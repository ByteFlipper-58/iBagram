package org.telegram.messenger.feature.maintabs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.maintabs.domain.usecase.GetMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.ObserveMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SelectMainTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetContactsPermissionWarningUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetMainTabsVisibleUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetShowCallsTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.UpdateChatsUnreadCountUseCase

class MainTabsViewModel(
    private val observeConfigUseCase: ObserveMainTabsConfigUseCase,
    private val getConfigUseCase: GetMainTabsConfigUseCase,
    private val setTabsVisibleUseCase: SetMainTabsVisibleUseCase,
    private val selectMainTabUseCase: SelectMainTabUseCase,
    private val setShowCallsTabUseCase: SetShowCallsTabUseCase,
    private val updateChatsUnreadCountUseCase: UpdateChatsUnreadCountUseCase,
    private val setContactsPermissionWarningUseCase: SetContactsPermissionWarningUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainTabsUiState(config = getConfigUseCase())
    )
    val uiState: StateFlow<MainTabsUiState> = _uiState.asStateFlow()

    init {
        observeConfigUseCase()
            .onEach { config ->
                _uiState.update { it.copy(config = config) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MainTabsEvent) {
        when (event) {
            is MainTabsEvent.SetTabsVisible -> {
                setTabsVisibleUseCase(event.visible)
            }
            is MainTabsEvent.SelectTab -> {
                selectMainTabUseCase(event.tab)
            }
            is MainTabsEvent.SelectPosition -> {
                selectMainTabUseCase(event.position)
            }
            is MainTabsEvent.SetShowCallsTab -> {
                setShowCallsTabUseCase(event.show)
            }
            is MainTabsEvent.UpdateUnreadCount -> {
                updateChatsUnreadCountUseCase(event.count)
            }
            is MainTabsEvent.SetContactsWarning -> {
                setContactsPermissionWarningUseCase(event.warning)
            }
        }
    }

    fun setTabsVisible(visible: Boolean) {
        onEvent(MainTabsEvent.SetTabsVisible(visible))
    }

    fun selectTab(tab: MainTabType) {
        onEvent(MainTabsEvent.SelectTab(tab))
    }

    fun selectPosition(position: Int) {
        onEvent(MainTabsEvent.SelectPosition(position))
    }

    fun setShowCallsTab(show: Boolean) {
        onEvent(MainTabsEvent.SetShowCallsTab(show))
    }

    fun updateUnreadCount(count: Int) {
        onEvent(MainTabsEvent.UpdateUnreadCount(count))
    }

    fun setContactsWarning(warning: Boolean) {
        onEvent(MainTabsEvent.SetContactsWarning(warning))
    }
}
