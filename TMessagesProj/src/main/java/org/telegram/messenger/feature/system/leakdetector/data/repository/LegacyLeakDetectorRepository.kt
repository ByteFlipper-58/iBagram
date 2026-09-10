package org.telegram.messenger.feature.system.leakdetector.data.repository

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
import org.telegram.messenger.feature.system.leakdetector.data.mapper.LeakDetectorMapper
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.system.leakdetector.domain.model.TrackedClassStats
import org.telegram.messenger.feature.system.leakdetector.domain.repository.LeakDetectorRepository
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe implementation of [LeakDetectorRepository] using Kotlin Coroutines
 * and WeakReferences.
 */
class LegacyLeakDetectorRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) : LeakDetectorRepository {

    private val lock = Any()
    private val registry = ConcurrentHashMap<String, CopyOnWriteArrayList<WeakReference<Any>>>()
    private val reportedLeaks = ConcurrentHashMap.newKeySet<String>()
    private val pendingRecheck = ConcurrentHashMap<String, Int>()

    private var activeConfig = LeakDetectorConfig()
    private var scanJob: Job? = null

    private val _stateFlow = MutableStateFlow(LeakDetectorState())
    private val _leakFlow = MutableSharedFlow<LeakReport>(replay = 1, extraBufferCapacity = 64)

    override fun start(config: LeakDetectorConfig) {
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

    override fun stop() {
        synchronized(lock) {
            scanJob?.cancel()
            scanJob = null
            updateStateLocked(isRunning = false)
        }
    }

    override fun track(tagOrClassName: String, instance: Any) {
        val list = registry.computeIfAbsent(tagOrClassName) { CopyOnWriteArrayList() }
        list.add(WeakReference(instance))
        synchronized(lock) {
            updateStateLocked()
        }
    }

    override fun triggerCheck(): List<LeakReport> {
        val newLeaks = mutableListOf<LeakReport>()
        val currentStats = cleanAndGetStats()

        synchronized(lock) {
            for (stat in currentStats) {
                if (reportedLeaks.contains(stat.className)) continue

                if (stat.liveCount >= activeConfig.leakThreshold) {
                    if (!pendingRecheck.containsKey(stat.className)) {
                        pendingRecheck[stat.className] = stat.liveCount
                        try {
                            System.gc()
                        } catch (_: Throwable) {}

                        val className = stat.className
                        scope.launch {
                            delay(activeConfig.gcRecheckDelayMs)
                            val report = confirmLeak(className)
                            if (report != null) {
                                _leakFlow.tryEmit(report)
                            }
                        }
                    }
                } else {
                    pendingRecheck.remove(stat.className)
                }
            }
            updateStateLocked()
        }

        return newLeaks
    }

    override fun confirmLeak(className: String): LeakReport? {
        synchronized(lock) {
            pendingRecheck.remove(className)
            if (reportedLeaks.contains(className)) return null

            val count = getLiveCount(className)
            if (count >= activeConfig.leakThreshold && reportedLeaks.add(className)) {
                val report = LeakDetectorMapper.buildLeakReport(
                    className = className,
                    instanceCount = count,
                    threshold = activeConfig.leakThreshold
                )
                updateStateLocked()
                return report
            }
            updateStateLocked()
            return null
        }
    }

    override fun getLiveCount(className: String): Int {
        val list = registry[className] ?: return 0
        var liveCount = 0
        val deadRefs = mutableListOf<WeakReference<Any>>()
        for (ref in list) {
            if (ref.get() != null) {
                liveCount++
            } else {
                deadRefs.add(ref)
            }
        }
        if (deadRefs.isNotEmpty()) {
            list.removeAll(deadRefs)
        }
        return liveCount
    }

    override fun getReportedLeaks(): List<LeakReport> {
        return _stateFlow.value.confirmedLeaks
    }

    override fun getTrackedStats(): List<TrackedClassStats> {
        return cleanAndGetStats()
    }

    override fun reset() {
        synchronized(lock) {
            scanJob?.cancel()
            scanJob = null
            registry.clear()
            reportedLeaks.clear()
            pendingRecheck.clear()
            _stateFlow.value = LeakDetectorState()
        }
    }

    override fun observeState(): StateFlow<LeakDetectorState> {
        return _stateFlow.asStateFlow()
    }

    override fun observeLeaks(): Flow<LeakReport> {
        return _leakFlow.asSharedFlow()
    }

    private fun cleanAndGetStats(): List<TrackedClassStats> {
        val stats = mutableListOf<TrackedClassStats>()
        for ((className, list) in registry) {
            var live = 0
            val dead = mutableListOf<WeakReference<Any>>()
            for (ref in list) {
                if (ref.get() != null) {
                    live++
                } else {
                    dead.add(ref)
                }
            }
            if (dead.isNotEmpty()) {
                list.removeAll(dead)
            }

            stats.add(
                LeakDetectorMapper.buildClassStats(
                    className = className,
                    liveCount = live,
                    threshold = activeConfig.leakThreshold,
                    isConfirmedLeak = reportedLeaks.contains(className),
                    isPendingRecheck = pendingRecheck.containsKey(className)
                )
            )
        }
        return stats
    }

    private fun updateStateLocked(isRunning: Boolean = scanJob?.isActive == true) {
        val stats = cleanAndGetStats()
        val currentReports = reportedLeaks.map { className ->
            LeakDetectorMapper.buildLeakReport(
                className = className,
                instanceCount = getLiveCount(className),
                threshold = activeConfig.leakThreshold
            )
        }
        _stateFlow.value = LeakDetectorMapper.buildState(
            isRunning = isRunning,
            stats = stats,
            confirmedLeaks = currentReports,
            pendingClasses = pendingRecheck.keys
        )
    }
}
