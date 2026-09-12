package org.telegram.messenger.feature.network.di

import org.telegram.messenger.feature.network.networkstats.data.repository.LegacyNetworkStatsRepository
import org.telegram.messenger.feature.network.networkstats.domain.repository.NetworkStatsRepository
import org.telegram.messenger.feature.network.networkstats.domain.usecase.CalculateMessagesTrafficUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatCallsDurationUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.GetAllNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.GetNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementCallsTimeUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficItemsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ObserveAllNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ObserveNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.RefreshNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ResetNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.presentation.NetworkStatsViewModel
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyLocalDataSource
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyRemoteDataSource
import org.telegram.messenger.feature.network.proxy.data.repository.LegacyProxyRepository
import org.telegram.messenger.feature.network.proxy.data.repository.ProxyRepositoryImpl
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository
import org.telegram.messenger.feature.network.proxy.domain.usecase.AddProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.CheckProxyPingUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.DeleteProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.DisableProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.EnableProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.GetProxySettingsUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.ObserveProxySettingsUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.ToggleProxyRotationUseCase
import org.telegram.messenger.feature.network.proxy.presentation.ProxyViewModel
import org.telegram.messenger.feature.network.push.data.repository.LegacyPushRepository
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository
import org.telegram.messenger.feature.network.push.domain.usecase.GetPushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.IsPushAvailableUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ObservePushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RegisterPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RequestPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ResetPushTokenUseCase
import org.telegram.messenger.feature.network.push.presentation.PushViewModel
import org.telegram.messenger.feature.network.pushlistener.data.repository.LegacyPushListenerRepository
import org.telegram.messenger.feature.network.pushlistener.domain.repository.PushListenerRepository
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.DeterminePushActionTypeUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.GetPushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObserveIncomingPushesUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObservePushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ParsePushJsonPayloadUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ProcessIncomingPushUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.RegisterPushListenerTokenUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.TogglePushListeningUseCase
import org.telegram.messenger.feature.network.pushlistener.presentation.PushListenerViewModel

class NetworkContainer(val account: Int) {

    val proxyRemoteDataSource: ProxyRemoteDataSource by lazy {
        ProxyRemoteDataSource(account)
    }

    val proxyLocalDataSource: ProxyLocalDataSource by lazy {
        ProxyLocalDataSource(account)
    }

    fun createProxyRepository(): ProxyRepository {
        return ProxyRepositoryImpl(
            account = account,
            localDataSource = proxyLocalDataSource,
            remoteDataSource = proxyRemoteDataSource
        )
    }

    private var customProxyRepository: ProxyRepository? = null

    var proxyRepository: ProxyRepository
        get() = customProxyRepository ?: createProxyRepository()
        set(value) {
            customProxyRepository = value
        }

    val observeProxySettingsUseCase: ObserveProxySettingsUseCase
        get() = ObserveProxySettingsUseCase(proxyRepository)

    val getProxySettingsUseCase: GetProxySettingsUseCase
        get() = GetProxySettingsUseCase(proxyRepository)

    val addProxyUseCase: AddProxyUseCase
        get() = AddProxyUseCase(proxyRepository)

    val deleteProxyUseCase: DeleteProxyUseCase
        get() = DeleteProxyUseCase(proxyRepository)

    val enableProxyUseCase: EnableProxyUseCase
        get() = EnableProxyUseCase(proxyRepository)

    val disableProxyUseCase: DisableProxyUseCase
        get() = DisableProxyUseCase(proxyRepository)

    val toggleProxyRotationUseCase: ToggleProxyRotationUseCase
        get() = ToggleProxyRotationUseCase(proxyRepository)

    val checkProxyPingUseCase: CheckProxyPingUseCase
        get() = CheckProxyPingUseCase(proxyRepository)

    private var cachedProxyViewModel: ProxyViewModel? = null

    val proxyViewModel: ProxyViewModel
        get() {
            var vm = cachedProxyViewModel
            if (vm == null) {
                vm = createProxyViewModel()
                cachedProxyViewModel = vm
            }
            return vm
        }

    fun createProxyViewModel(): ProxyViewModel {
        return ProxyViewModel(
            observeProxySettingsUseCase = observeProxySettingsUseCase,
            getProxySettingsUseCase = getProxySettingsUseCase,
            addProxyUseCase = addProxyUseCase,
            deleteProxyUseCase = deleteProxyUseCase,
            enableProxyUseCase = enableProxyUseCase,
            disableProxyUseCase = disableProxyUseCase,
            toggleProxyRotationUseCase = toggleProxyRotationUseCase,
            checkProxyPingUseCase = checkProxyPingUseCase
        )
    }

    private var customPushRepository: PushRepository? = null

    var pushRepository: PushRepository
        get() = customPushRepository ?: LegacyPushRepository(account)
        set(value) {
            customPushRepository = value
        }

    val observePushStatusUseCase: ObservePushStatusUseCase
        get() = ObservePushStatusUseCase(pushRepository)

    val getPushStatusUseCase: GetPushStatusUseCase
        get() = GetPushStatusUseCase(pushRepository)

    val isPushAvailableUseCase: IsPushAvailableUseCase
        get() = IsPushAvailableUseCase(pushRepository)

    val requestPushTokenUseCase: RequestPushTokenUseCase
        get() = RequestPushTokenUseCase(pushRepository)

    val registerPushTokenUseCase: RegisterPushTokenUseCase
        get() = RegisterPushTokenUseCase(pushRepository)

    val resetPushTokenUseCase: ResetPushTokenUseCase
        get() = ResetPushTokenUseCase(pushRepository)

    private var cachedPushViewModel: PushViewModel? = null

    val pushViewModel: PushViewModel
        get() {
            var vm = cachedPushViewModel
            if (vm == null) {
                vm = createPushViewModel()
                cachedPushViewModel = vm
            }
            return vm
        }

    fun createPushViewModel(): PushViewModel {
        return PushViewModel(
            observePushStatusUseCase = observePushStatusUseCase,
            getPushStatusUseCase = getPushStatusUseCase,
            isPushAvailableUseCase = isPushAvailableUseCase,
            requestPushTokenUseCase = requestPushTokenUseCase,
            registerPushTokenUseCase = registerPushTokenUseCase,
            resetPushTokenUseCase = resetPushTokenUseCase
        )
    }

    private var customNetworkStatsRepository: NetworkStatsRepository? = null

    var networkStatsRepository: NetworkStatsRepository
        get() = customNetworkStatsRepository ?: LegacyNetworkStatsRepository(account)
        set(value) {
            customNetworkStatsRepository = value
        }

    val observeNetworkStatsUseCase: ObserveNetworkStatsUseCase
        get() = ObserveNetworkStatsUseCase(networkStatsRepository)

    val observeAllNetworkStatsUseCase: ObserveAllNetworkStatsUseCase
        get() = ObserveAllNetworkStatsUseCase(networkStatsRepository)

    val getNetworkStatsUseCase: GetNetworkStatsUseCase
        get() = GetNetworkStatsUseCase(networkStatsRepository)

    val getAllNetworkStatsUseCase: GetAllNetworkStatsUseCase
        get() = GetAllNetworkStatsUseCase(networkStatsRepository)

    val incrementTrafficBytesUseCase: IncrementTrafficBytesUseCase
        get() = IncrementTrafficBytesUseCase(networkStatsRepository)

    val incrementTrafficItemsUseCase: IncrementTrafficItemsUseCase
        get() = IncrementTrafficItemsUseCase(networkStatsRepository)

    val incrementCallsTimeUseCase: IncrementCallsTimeUseCase
        get() = IncrementCallsTimeUseCase(networkStatsRepository)

    val resetNetworkStatsUseCase: ResetNetworkStatsUseCase
        get() = ResetNetworkStatsUseCase(networkStatsRepository)

    val refreshNetworkStatsUseCase: RefreshNetworkStatsUseCase
        get() = RefreshNetworkStatsUseCase(networkStatsRepository)

    val calculateMessagesTrafficUseCase: CalculateMessagesTrafficUseCase
        get() = CalculateMessagesTrafficUseCase()

    val formatTrafficBytesUseCase: FormatTrafficBytesUseCase
        get() = FormatTrafficBytesUseCase()

    val formatCallsDurationUseCase: FormatCallsDurationUseCase
        get() = FormatCallsDurationUseCase()

    private var cachedNetworkStatsViewModel: NetworkStatsViewModel? = null

    val networkStatsViewModel: NetworkStatsViewModel
        get() {
            var vm = cachedNetworkStatsViewModel
            if (vm == null) {
                vm = createNetworkStatsViewModel()
                cachedNetworkStatsViewModel = vm
            }
            return vm
        }

    fun createNetworkStatsViewModel(): NetworkStatsViewModel {
        return NetworkStatsViewModel(
            observeAllNetworkStatsUseCase = observeAllNetworkStatsUseCase,
            getNetworkStatsUseCase = getNetworkStatsUseCase,
            resetNetworkStatsUseCase = resetNetworkStatsUseCase,
            refreshNetworkStatsUseCase = refreshNetworkStatsUseCase,
            incrementTrafficBytesUseCase = incrementTrafficBytesUseCase,
            incrementTrafficItemsUseCase = incrementTrafficItemsUseCase,
            incrementCallsTimeUseCase = incrementCallsTimeUseCase,
            formatTrafficBytesUseCase = formatTrafficBytesUseCase,
            formatCallsDurationUseCase = formatCallsDurationUseCase
        )
    }

    private var customPushListenerRepository: PushListenerRepository? = null

    var pushListenerRepository: PushListenerRepository
        get() = customPushListenerRepository ?: LegacyPushListenerRepository(account)
        set(value) {
            customPushListenerRepository = value
        }

    val observePushListenerStateUseCase: ObservePushListenerStateUseCase
        get() = ObservePushListenerStateUseCase(pushListenerRepository)

    val observeIncomingPushesUseCase: ObserveIncomingPushesUseCase
        get() = ObserveIncomingPushesUseCase(pushListenerRepository)

    val getPushListenerStateUseCase: GetPushListenerStateUseCase
        get() = GetPushListenerStateUseCase(pushListenerRepository)

    val processIncomingPushUseCase: ProcessIncomingPushUseCase
        get() = ProcessIncomingPushUseCase(pushListenerRepository)

    val registerPushListenerTokenUseCase: RegisterPushListenerTokenUseCase
        get() = RegisterPushListenerTokenUseCase(pushListenerRepository)

    val togglePushListeningUseCase: TogglePushListeningUseCase
        get() = TogglePushListeningUseCase(pushListenerRepository)

    val determinePushActionTypeUseCase: DeterminePushActionTypeUseCase
        get() = DeterminePushActionTypeUseCase()

    val parsePushJsonPayloadUseCase: ParsePushJsonPayloadUseCase
        get() = ParsePushJsonPayloadUseCase()

    private var cachedPushListenerViewModel: PushListenerViewModel? = null

    val pushListenerViewModel: PushListenerViewModel
        get() {
            var vm = cachedPushListenerViewModel
            if (vm == null) {
                vm = createPushListenerViewModel()
                cachedPushListenerViewModel = vm
            }
            return vm
        }

    fun createPushListenerViewModel(): PushListenerViewModel {
        return PushListenerViewModel(
            observeStateUseCase = observePushListenerStateUseCase,
            processPushUseCase = processIncomingPushUseCase,
            registerTokenUseCase = registerPushListenerTokenUseCase,
            toggleListeningUseCase = togglePushListeningUseCase
        )
    }

    // --- Feature: Browser (Slice #86) ---
}
