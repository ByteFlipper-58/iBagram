package org.telegram.messenger.feature.system.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetBadgeSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetNotificationSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.MuteDialogUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveBadgeSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveNotificationSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.RefreshBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleContactJoinedNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppPreviewUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppSoundsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppVibrateUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInChatSoundUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.TogglePeerNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.TogglePinnedMessagesNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.UpdateBadgeSettingsUseCase

class NotificationsViewModel(
    private val observeNotificationSettingsUseCase: ObserveNotificationSettingsUseCase,
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val observeBadgeUseCase: ObserveBadgeUseCase,
    private val getBadgeUseCase: GetBadgeUseCase,
    private val observeBadgeSettingsUseCase: ObserveBadgeSettingsUseCase,
    private val getBadgeSettingsUseCase: GetBadgeSettingsUseCase,
    private val togglePeerNotificationsUseCase: TogglePeerNotificationsUseCase,
    private val toggleInChatSoundUseCase: ToggleInChatSoundUseCase,
    private val toggleInAppSoundsUseCase: ToggleInAppSoundsUseCase,
    private val toggleInAppVibrateUseCase: ToggleInAppVibrateUseCase,
    private val toggleInAppPreviewUseCase: ToggleInAppPreviewUseCase,
    private val toggleContactJoinedNotificationsUseCase: ToggleContactJoinedNotificationsUseCase,
    private val togglePinnedMessagesNotificationsUseCase: TogglePinnedMessagesNotificationsUseCase,
    private val updateBadgeSettingsUseCase: UpdateBadgeSettingsUseCase,
    private val muteDialogUseCase: MuteDialogUseCase,
    private val refreshBadgeUseCase: RefreshBadgeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationsEvent>()
    val events: SharedFlow<NotificationsEvent> = _events.asSharedFlow()

    init {
        loadInitialState()
        observeStreams()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val settings = getNotificationSettingsUseCase()
            val badgeSettings = getBadgeSettingsUseCase()
            val badgeCount = getBadgeUseCase()

            _uiState.value = _uiState.value.copy(
                settings = settings,
                badgeSettings = badgeSettings,
                badgeCount = badgeCount,
                isLoading = false
            )
        }
    }

    private fun observeStreams() {
        viewModelScope.launch {
            observeNotificationSettingsUseCase().collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            observeBadgeSettingsUseCase().collect { badgeSettings ->
                _uiState.value = _uiState.value.copy(badgeSettings = badgeSettings)
            }
        }
        viewModelScope.launch {
            observeBadgeUseCase().collect { badgeCount ->
                _uiState.value = _uiState.value.copy(badgeCount = badgeCount)
            }
        }
    }

    fun onEvent(event: NotificationsEvent) {
        when (event) {
            is NotificationsEvent.TogglePeerType -> {
                viewModelScope.launch {
                    when (val result = togglePeerNotificationsUseCase(event.type, event.enabled)) {
                        is Result.Success -> {
                            val current = _uiState.value.settings
                            val updated = when (event.type) {
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.PRIVATE_CHATS ->
                                    current.copy(privateChatsEnabled = event.enabled)
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.GROUPS ->
                                    current.copy(groupsEnabled = event.enabled)
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.CHANNELS ->
                                    current.copy(channelsEnabled = event.enabled)
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.STORIES ->
                                    current.copy(storiesEnabled = event.enabled)
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.REACTIONS_MESSAGES ->
                                    current.copy(reactionsMessagesEnabled = event.enabled)
                                org.telegram.messenger.feature.system.notifications.domain.model.NotificationPeerType.REACTIONS_STORIES ->
                                    current.copy(reactionsStoriesEnabled = event.enabled)
                            }
                            _uiState.value = _uiState.value.copy(settings = updated)
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ToggleInChatSound -> {
                viewModelScope.launch {
                    when (val result = toggleInChatSoundUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(inChatSoundEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ToggleInAppSounds -> {
                viewModelScope.launch {
                    when (val result = toggleInAppSoundsUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(inAppSoundsEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ToggleInAppVibrate -> {
                viewModelScope.launch {
                    when (val result = toggleInAppVibrateUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(inAppVibrateEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ToggleInAppPreview -> {
                viewModelScope.launch {
                    when (val result = toggleInAppPreviewUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(inAppPreviewEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ToggleContactJoined -> {
                viewModelScope.launch {
                    when (val result = toggleContactJoinedNotificationsUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(contactJoinedNotificationsEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.TogglePinnedMessages -> {
                viewModelScope.launch {
                    when (val result = togglePinnedMessagesNotificationsUseCase(event.enabled)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                settings = _uiState.value.settings.copy(pinnedMessagesNotificationsEnabled = event.enabled)
                            )
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.UpdateBadgeSettings -> {
                viewModelScope.launch {
                    when (val result = updateBadgeSettingsUseCase(event.settings)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(badgeSettings = event.settings)
                        }
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.MuteDialog -> {
                viewModelScope.launch {
                    when (val result = muteDialogUseCase(event.dialogId, event.topicId, event.mute)) {
                        is Result.Success -> {}
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.MuteDialogUntil -> {
                viewModelScope.launch {
                    when (val result = muteDialogUseCase.until(event.dialogId, event.topicId, event.untilDate)) {
                        is Result.Success -> {}
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.RefreshBadge -> {
                viewModelScope.launch {
                    when (val result = refreshBadgeUseCase()) {
                        is Result.Success -> {}
                        is Result.Failure -> {
                            _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                        }
                    }
                }
            }

            is NotificationsEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }
    }
}
