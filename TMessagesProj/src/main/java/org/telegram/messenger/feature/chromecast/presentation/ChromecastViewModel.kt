package org.telegram.messenger.feature.chromecast.presentation

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
import org.telegram.messenger.feature.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.chromecast.domain.usecase.CastMediaUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.GetChromecastStateUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.IsCastingUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.IsMediaPlayingOnCastUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.ObserveChromecastStateUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.SetCastCoverFileUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.StopCastingUseCase
import java.io.File

class ChromecastViewModel(
    private val observeChromecastStateUseCase: ObserveChromecastStateUseCase,
    private val getChromecastStateUseCase: GetChromecastStateUseCase,
    private val isCastingUseCase: IsCastingUseCase,
    private val isMediaPlayingOnCastUseCase: IsMediaPlayingOnCastUseCase,
    private val castMediaUseCase: CastMediaUseCase,
    private val stopCastingUseCase: StopCastingUseCase,
    private val setCastCoverFileUseCase: SetCastCoverFileUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _uiState = MutableStateFlow(
        ChromecastUiState(state = getChromecastStateUseCase())
    )
    val uiState: StateFlow<ChromecastUiState> = _uiState.asStateFlow()

    init {
        observeChromecastStateUseCase()
            .onEach { state ->
                _uiState.update { it.copy(state = state) }
            }
            .launchIn(scope)
    }

    fun onEvent(event: ChromecastEvent) {
        when (event) {
            ChromecastEvent.RefreshState -> refreshState()
            is ChromecastEvent.CastMedia -> castMedia(event.media)
            ChromecastEvent.StopCasting -> stopCasting()
            is ChromecastEvent.SetCoverFile -> setCoverFile(event.file)
            ChromecastEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            ChromecastEvent.DismissInfo -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private fun refreshState() {
        val state = getChromecastStateUseCase()
        _uiState.update { it.copy(state = state) }
    }

    private fun castMedia(media: ChromecastMediaModel) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            castMediaUseCase(media)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Media casting started"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to cast media"
                        )
                    }
                }
        }
    }

    private fun stopCasting() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            stopCastingUseCase()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Casting stopped"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to stop casting"
                        )
                    }
                }
        }
    }

    private fun setCoverFile(file: File?) {
        scope.launch {
            setCastCoverFileUseCase(file)
                .onSuccess { path ->
                    _uiState.update {
                        it.copy(infoMessage = path?.let { "Cover updated: $it" })
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to set cover")
                    }
                }
        }
    }
}
