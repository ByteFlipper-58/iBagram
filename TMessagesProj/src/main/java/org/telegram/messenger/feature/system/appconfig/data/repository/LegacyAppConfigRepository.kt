package org.telegram.messenger.feature.system.appconfig.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.AppGlobalConfig
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.system.appconfig.data.mapper.AppConfigMapper
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState
import org.telegram.messenger.feature.system.appconfig.domain.repository.AppConfigRepository

class LegacyAppConfigRepository(
    private val currentAccount: Int = 0,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppConfigRepository, NotificationCenter.NotificationCenterDelegate {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _configState = MutableStateFlow(loadCurrentState())

    init {
        runCatching {
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.appConfigUpdated)
        }
    }

    private fun loadCurrentState(): AppGlobalConfigState {
        val config = runCatching { AppGlobalConfig.getInstance(currentAccount) }.getOrNull()
        val controller = runCatching { MessagesController.getInstance(currentAccount) }.getOrNull()
        return AppConfigMapper.fromAppGlobalConfig(config, controller)
    }

    override fun observeConfig(): Flow<AppGlobalConfigState> = _configState.asStateFlow()

    override fun getConfig(): AppGlobalConfigState = _configState.value

    override suspend fun reloadConfig(): Result<AppGlobalConfigState> = withContext(ioDispatcher) {
        runCatching {
            val newState = loadCurrentState()
            _configState.value = newState
            newState
        }
    }

    override suspend fun updateConfigValue(key: String, value: Any): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val current = _configState.value
            val newCustom = current.customEntries.toMutableMap().apply {
                put(key, value)
            }
            _configState.value = current.copy(customEntries = newCustom)
        }
    }

    fun updateInMemoryState(state: AppGlobalConfigState) {
        _configState.value = state
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.appConfigUpdated && (account == currentAccount || account == -1)) {
            scope.launch {
                reloadConfig()
            }
        }
    }

    fun destroy() {
        runCatching {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.appConfigUpdated)
        }
    }
}
