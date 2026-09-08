package org.telegram.messenger.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.profile.domain.usecase.BlockPeerUseCase
import org.telegram.messenger.feature.profile.domain.usecase.LoadFullProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.ObserveProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.UnblockPeerUseCase

/**
 * ViewModel managing UI state and operations for the peer profile screen.
 */
class ProfileViewModel(
    val account: Int,
    val peerId: Long,
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val loadFullProfileUseCase: LoadFullProfileUseCase,
    private val blockPeerUseCase: BlockPeerUseCase,
    private val unblockPeerUseCase: UnblockPeerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    private var observeJob: Job? = null

    init {
        observeProfile()
    }

    private fun observeProfile() {
        observeJob?.cancel()
        _uiState.value = ProfileUiState.Loading

        observeJob = observeProfileUseCase(peerId)
            .onEach { profile ->
                if (profile != null) {
                    val current = _uiState.value
                    val isUpdating = (current as? ProfileUiState.Success)?.isUpdatingBlockState ?: false
                    _uiState.value = ProfileUiState.Success(
                        profile = profile,
                        isUpdatingBlockState = isUpdating
                    )
                } else {
                    _uiState.value = ProfileUiState.Error("Profile not found")
                }
            }
            .catch { error ->
                _uiState.value = ProfileUiState.Error(error.message)
            }
            .launchIn(viewModelScope)
    }

    fun onLoadFullProfile() {
        viewModelScope.launch {
            val result = loadFullProfileUseCase(peerId)
            if (result is Result.Failure) {
                _events.emit(ProfileEvent.ShowError(result.error.message))
            }
        }
    }

    fun onToggleBlock() {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        if (current.isUpdatingBlockState) return

        val willBlock = !current.profile.isBlocked
        _uiState.value = current.copy(isUpdatingBlockState = true)

        viewModelScope.launch {
            val result = if (willBlock) {
                blockPeerUseCase(peerId)
            } else {
                unblockPeerUseCase(peerId)
            }

            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.BlockStateChanged(willBlock))
                }
                is Result.Failure -> {
                    _events.emit(ProfileEvent.ShowError(result.error.message))
                }
            }

            val latest = _uiState.value
            if (latest is ProfileUiState.Success) {
                _uiState.value = latest.copy(isUpdatingBlockState = false)
            }
        }
    }
}
