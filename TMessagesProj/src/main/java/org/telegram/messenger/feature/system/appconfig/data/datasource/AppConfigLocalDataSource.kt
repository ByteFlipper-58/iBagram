package org.telegram.messenger.feature.system.appconfig.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.AppGlobalConfig
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.system.appconfig.data.mapper.AppConfigMapper
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState

/**
 * Local data source managing in-memory cached app configuration state,
 * NotificationCenter updates, and headless testing fallback.
 */
class AppConfigLocalDataSource(
    private val currentAccount: Int = 0,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    var testMode: Boolean = false
) : NotificationCenter.NotificationCenterDelegate {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _configState = MutableStateFlow(loadCurrentState())
    val configState: StateFlow<AppGlobalConfigState> = _configState.asStateFlow()

    init {
        if (!testMode) {
            runCatching {
                NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.appConfigUpdated)
            }
        }
    }

    fun loadCurrentState(): AppGlobalConfigState {
        if (testMode) {
            return _configState?.value ?: AppGlobalConfigState()
        }
        val config = runCatching { AppGlobalConfig.getInstance(currentAccount) }.getOrNull()
        val controller = runCatching { MessagesController.getInstance(currentAccount) }.getOrNull()
        return if (config != null || controller != null) {
            AppConfigMapper.fromAppGlobalConfig(config, controller)
        } else {
            _configState?.value ?: AppGlobalConfigState()
        }
    }

    fun observeConfig(): Flow<AppGlobalConfigState> = _configState.asStateFlow()

    fun getConfig(): AppGlobalConfigState = _configState.value

    suspend fun reloadConfig(): Result<AppGlobalConfigState> = withContext(ioDispatcher) {
        runCatching {
            val newState = loadCurrentState()
            _configState.value = newState
            newState
        }
    }

    suspend fun updateConfigValue(key: String, value: Any): Result<Unit> = withContext(ioDispatcher) {
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
        if (!testMode) {
            runCatching {
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.appConfigUpdated)
            }
        }
    }
}
