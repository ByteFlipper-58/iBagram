package org.telegram.messenger.feature.system.leakdetector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.leakdetector.data.datasource.LeakDetectorLocalDataSource
import org.telegram.messenger.feature.system.leakdetector.data.datasource.LeakDetectorRemoteDataSource
import org.telegram.messenger.feature.system.leakdetector.data.repository.LeakDetectorRepositoryImpl
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorConfig

class LeakDetectorRepositoryImplTest {

    private lateinit var localDataSource: LeakDetectorLocalDataSource
    private lateinit var remoteDataSource: LeakDetectorRemoteDataSource
    private lateinit var repository: LeakDetectorRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = LeakDetectorLocalDataSource()
        remoteDataSource = LeakDetectorRemoteDataSource(currentAccount = 0)
        repository = LeakDetectorRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialState_isNotRunningAndEmpty() {
        val state = repository.observeState().value
        assertFalse(state.isRunning)
        assertEquals(0, state.trackedClassesCount)
        assertEquals(0, state.totalLiveInstances)
        assertEquals(0, state.suspiciousClassesCount)
        assertTrue(state.confirmedLeaks.isEmpty())
        assertTrue(repository.getReportedLeaks().isEmpty())
        assertTrue(repository.getTrackedStats().isEmpty())
    }

    @Test
    fun startAndStop_updatesRunningState() {
        repository.start(LeakDetectorConfig(leakThreshold = 3))
        assertTrue(repository.observeState().value.isRunning)

        repository.stop()
        assertFalse(repository.observeState().value.isRunning)
    }

    @Test
    fun trackObject_incrementsLiveCount() {
        val dummy1 = Any()
        val dummy2 = Any()

        repository.track("SampleActivity", dummy1)
        repository.track("SampleActivity", dummy2)

        assertEquals(2, repository.getLiveCount("SampleActivity"))
        val state = repository.observeState().value
        assertEquals(1, state.trackedClassesCount)
        assertEquals(2, state.totalLiveInstances)
    }

    @Test
    fun triggerCheck_detectsLeakWhenThresholdExceeded() {
        repository.start(LeakDetectorConfig(leakThreshold = 3))

        val objects = listOf(Any(), Any(), Any())
        for (obj in objects) {
            repository.track("LeakyFragment", obj)
        }

        val detected = repository.triggerCheck()
        assertEquals(1, detected.size)
        assertEquals("LeakyFragment", detected[0].className)
        assertEquals(3, detected[0].instanceCount)

        val state = repository.observeState().value
        assertEquals(1, state.confirmedLeaks.size)
        assertEquals("LeakyFragment", state.confirmedLeaks[0].className)
    }

    @Test
    fun confirmLeak_createsReportForLeakedClass() {
        repository.start(LeakDetectorConfig(leakThreshold = 2))

        val obj1 = Any()
        val obj2 = Any()
        repository.track("LeakyView", obj1)
        repository.track("LeakyView", obj2)

        val report = repository.confirmLeak("LeakyView")
        assertNotNull(report)
        assertEquals("LeakyView", report!!.className)
        assertEquals(2, report.instanceCount)

        // Below threshold returns null
        val notLeakedReport = repository.confirmLeak("NonExistentClass")
        assertNull(notLeakedReport)
    }

    @Test
    fun getTrackedStats_returnsAccurateStats() {
        repository.start(LeakDetectorConfig(leakThreshold = 3))

        val obj1 = Any()
        repository.track("NormalService", obj1)

        val stats = repository.getTrackedStats()
        assertEquals(1, stats.size)
        assertEquals("NormalService", stats[0].className)
        assertEquals(1, stats[0].liveCount)
        assertFalse(stats[0].isSuspicious)
        assertFalse(stats[0].isConfirmedLeak)
    }

    @Test
    fun reset_clearsAllTrackingAndLeaks() {
        val obj = Any()
        repository.track("TemporaryComponent", obj)
        assertEquals(1, repository.getLiveCount("TemporaryComponent"))

        repository.reset()
        assertEquals(0, repository.getLiveCount("TemporaryComponent"))
        assertTrue(repository.getReportedLeaks().isEmpty())
        assertTrue(repository.getTrackedStats().isEmpty())
        assertEquals(0, repository.observeState().value.totalLiveInstances)
    }
}
