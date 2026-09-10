package org.telegram.messenger.feature.network.networkstats.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.network.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.network.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatCallsDurationUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.GetNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementCallsTimeUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficItemsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ObserveAllNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.RefreshNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ResetNetworkStatsUseCase

class NetworkStatsViewModel(
    private val observeAllNetworkStatsUseCase: ObserveAllNetworkStatsUseCase,
    private val getNetworkStatsUseCase: GetNetworkStatsUseCase,
    private val resetNetworkStatsUseCase: ResetNetworkStatsUseCase,
    private val refreshNetworkStatsUseCase: RefreshNetworkStatsUseCase,
    private val incrementTrafficBytesUseCase: IncrementTrafficBytesUseCase,
    private val incrementTrafficItemsUseCase: IncrementTrafficItemsUseCase,
    private val incrementCallsTimeUseCase: IncrementCallsTimeUseCase,
    private val formatTrafficBytesUseCase: FormatTrafficBytesUseCase = FormatTrafficBytesUseCase(),
    private val formatCallsDurationUseCase: FormatCallsDurationUseCase = FormatCallsDurationUseCase(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(NetworkStatsUiState())
    val uiState: StateFlow<NetworkStatsUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        observeAllNetworkStatsUseCase()
            .onEach { allSummaries ->
                val formattedMap = allSummaries.mapValues { (_, summary) ->
                    formatSummary(summary)
                }
                _uiState.update { current ->
                    current.copy(
                        allSummaries = formattedMap,
                        currentSummary = formattedMap[current.selectedNetworkType],
                        isLoading = false
                    )
                }
            }
            .launchIn(scope)
    }

    fun onEvent(event: NetworkStatsEvent) {
        when (event) {
            is NetworkStatsEvent.SelectNetworkType -> {
                _uiState.update { current ->
                    current.copy(
                        selectedNetworkType = event.networkType,
                        currentSummary = current.allSummaries[event.networkType]
                    )
                }
            }
            is NetworkStatsEvent.ResetStats -> {
                scope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        resetNetworkStatsUseCase(event.networkType)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = "Statistics reset for ${event.networkType.name}"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
            }
            is NetworkStatsEvent.Refresh -> {
                scope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        refreshNetworkStatsUseCase()
                        _uiState.update { it.copy(isLoading = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
            }
            is NetworkStatsEvent.IncrementTraffic -> {
                scope.launch {
                    try {
                        incrementTrafficBytesUseCase(
                            networkType = event.networkType,
                            category = event.category,
                            sentBytes = event.sentBytes,
                            receivedBytes = event.receivedBytes
                        )
                        incrementTrafficItemsUseCase(
                            networkType = event.networkType,
                            category = event.category,
                            sentItems = event.sentItems,
                            receivedItems = event.receivedItems
                        )
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            is NetworkStatsEvent.IncrementCallsTime -> {
                scope.launch {
                    try {
                        incrementCallsTimeUseCase(event.networkType, event.seconds)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
            }
            NetworkStatsEvent.ClearMessage -> {
                _uiState.update { it.copy(infoMessage = null, error = null) }
            }
        }
    }

    private fun formatSummary(summary: NetworkStatsSummaryModel): FormattedNetworkStatsSummary {
        val formattedItems = summary.items.mapValues { (_, item) ->
            FormattedTrafficItem(
                category = item.category,
                sentBytesFormatted = formatTrafficBytesUseCase(item.sentBytes),
                receivedBytesFormatted = formatTrafficBytesUseCase(item.receivedBytes),
                totalBytesFormatted = formatTrafficBytesUseCase(item.totalBytes),
                sentItemsCount = item.sentItems,
                receivedItemsCount = item.receivedItems,
                totalItemsCount = item.totalItems
            )
        }

        return FormattedNetworkStatsSummary(
            networkType = summary.networkType,
            items = formattedItems,
            callsDurationFormatted = formatCallsDurationUseCase(summary.callsTotalTimeSec),
            resetStatsDateMs = summary.resetStatsDateMs,
            totalSentFormatted = formatTrafficBytesUseCase(summary.totalSentBytes),
            totalReceivedFormatted = formatTrafficBytesUseCase(summary.totalReceivedBytes),
            grandTotalFormatted = formatTrafficBytesUseCase(summary.totalBytes)
        )
    }

    fun onCleared() {
        scope.cancel()
    }
}
