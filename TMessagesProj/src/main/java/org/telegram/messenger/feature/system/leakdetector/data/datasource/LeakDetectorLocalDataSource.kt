package org.telegram.messenger.feature.system.leakdetector.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.system.leakdetector.domain.model.TrackedClassStats
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local data source for memory leak detection, WeakReference tracking,
 * two-phase GC rechecks, and leak state management.
 */
class LeakDetectorLocalDataSource(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) {
    private val lock = Any()
    private val registry = ConcurrentHashMap<String, CopyOnWriteArrayList<WeakReference<Any>>>()
    private val reportedLeaks = ConcurrentHashMap.newKeySet<String>()
    private val pendingRecheck = ConcurrentHashMap<String, Int>()

    private var activeConfig = LeakDetectorConfig()
    private var scanJob: Job? = null

    private val _stateFlow = MutableStateFlow(LeakDetectorState())
    val stateFlow: StateFlow<LeakDetectorState> = _stateFlow.asStateFlow()

    private val _leakFlow = MutableSharedFlow<LeakReport>(replay = 1, extraBufferCapacity = 64)
    val leakFlow: Flow<LeakReport> = _leakFlow.asSharedFlow()

    fun start(config: LeakDetectorConfig = LeakDetectorConfig()) {
        synchronized(lock) {
            activeConfig = config
            scanJob?.cancel()
            val job = scope.launch {
                while (isActive) {
                    delay(config.checkIntervalMs)
                    triggerCheck()
                }
            }
            scanJob = job
            updateStateLocked(isRunning = true)
        }
    }

    fun stop() {
        synchronized(lock) {
            scanJob?.cancel()
            scanJob = null
            updateStateLocked(isRunning = false)
        }
    }

    fun track(tagOrClassName: String, instance: Any) {
        val list = registry.computeIfAbsent(tagOrClassName) { CopyOnWriteArrayList() }
        list.add(WeakReference(instance))
        synchronized(lock) {
            updateStateLocked(isRunning = scanJob != null)
        }
    }

    fun triggerCheck(): List<LeakReport> {
        val newlyDetected = mutableListOf<LeakReport>()

        synchronized(lock) {
            cleanDeadReferences()

            for ((className, list) in registry) {
                val liveCount = list.size
                if (liveCount >= activeConfig.leakThreshold) {
                    if (!reportedLeaks.contains(className)) {
                        pendingRecheck[className] = liveCount
                    }
                } else {
                    pendingRecheck.remove(className)
                }
            }

            for (className in pendingRecheck.keys) {
                val currentLive = getLiveCount(className)
                if (currentLive >= activeConfig.leakThreshold && !reportedLeaks.contains(className)) {
                    val report = LeakReport(
                        className = className,
                        instanceCount = currentLive,
                        threshold = activeConfig.leakThreshold
                    )
                    reportedLeaks.add(className)
                    newlyDetected.add(report)
                }
            }

            updateStateLocked(isRunning = scanJob != null)
        }

        for (report in newlyDetected) {
            _leakFlow.tryEmit(report)
        }

        return newlyDetected
    }

    fun confirmLeak(className: String): LeakReport? {
        synchronized(lock) {
            cleanDeadReferences()
            val liveCount = getLiveCount(className)
            if (liveCount >= activeConfig.leakThreshold) {
                reportedLeaks.add(className)
                val report = LeakReport(
                    className = className,
                    instanceCount = liveCount,
                    threshold = activeConfig.leakThreshold
                )
                updateStateLocked(isRunning = scanJob != null)
                _leakFlow.tryEmit(report)
                return report
            }
            return null
        }
    }

    fun getLiveCount(className: String): Int {
        val list = registry[className] ?: return 0
        var count = 0
        for (ref in list) {
            if (ref.get() != null) {
                count++
            }
        }
        return count
    }

    fun getReportedLeaks(): List<LeakReport> {
        synchronized(lock) {
            return reportedLeaks.map { className ->
                LeakReport(
                    className = className,
                    instanceCount = getLiveCount(className),
                    threshold = activeConfig.leakThreshold
                )
            }
        }
    }

    fun getTrackedStats(): List<TrackedClassStats> {
        synchronized(lock) {
            cleanDeadReferences()
            return registry.map { (className, list) ->
                val count = list.size
                TrackedClassStats(
                    className = className,
                    liveCount = count,
                    isSuspicious = count >= activeConfig.leakThreshold,
                    isConfirmedLeak = reportedLeaks.contains(className),
                    isPendingRecheck = pendingRecheck.containsKey(className)
                )
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            registry.clear()
            reportedLeaks.clear()
            pendingRecheck.clear()
            updateStateLocked(isRunning = scanJob != null)
        }
    }

    fun observeState(): StateFlow<LeakDetectorState> = stateFlow

    fun observeLeaks(): Flow<LeakReport> = leakFlow

    private fun cleanDeadReferences() {
        for ((className, list) in registry) {
            val live = list.filter { it.get() != null }
            if (live.isEmpty()) {
                registry.remove(className)
            } else {
                registry[className] = CopyOnWriteArrayList(live)
            }
        }
    }

    private fun updateStateLocked(isRunning: Boolean) {
        var totalLive = 0
        var suspicious = 0

        for ((_, list) in registry) {
            val count = list.size
            totalLive += count
            if (count >= activeConfig.leakThreshold) {
                suspicious++
            }
        }

        val reports = reportedLeaks.map { className ->
            LeakReport(
                className = className,
                instanceCount = getLiveCount(className),
                threshold = activeConfig.leakThreshold
            )
        }

        _stateFlow.value = LeakDetectorState(
            isRunning = isRunning,
            trackedClassesCount = registry.size,
            totalLiveInstances = totalLive,
            suspiciousClassesCount = suspicious,
            confirmedLeaks = reports,
            pendingRecheckClasses = pendingRecheck.keys.toList(),
            lastCheckTimestampMs = System.currentTimeMillis()
        )
    }
}
