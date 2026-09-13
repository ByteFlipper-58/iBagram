package org.telegram.messenger.feature.business.timezones

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.timezones.data.datasource.TimezonesLocalDataSource
import org.telegram.messenger.feature.business.timezones.data.datasource.TimezonesRemoteDataSource
import org.telegram.messenger.feature.business.timezones.data.repository.TimezonesRepositoryImpl
import org.telegram.tgnet.TLRPC

class TimezonesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTimezonesRemoteDataSource(account: Int) : TimezonesRemoteDataSource(account) {
        var remoteTimezones = mutableListOf<TLRPC.TL_timezone>()
        var shouldFail = false

        override suspend fun loadTimezones(hash: Int): Result<List<TLRPC.TL_timezone>> {
            if (shouldFail) {
                return Result.failure("Remote error")
            }
            return Result.success(remoteTimezones)
        }
    }

    private fun createSampleTlTimezone(id: String, name: String, offset: Int): TLRPC.TL_timezone {
        return TLRPC.TL_timezone().apply {
            this.id = id
            this.name = name
            this.utc_offset = offset
        }
    }

    @Test
    fun testGetTimezonesFromLocalCache() = runTest(testDispatcher) {
        val local = TimezonesLocalDataSource(0)
        val remote = FakeTimezonesRemoteDataSource(0)
        val repo = TimezonesRepositoryImpl(0, local, remote, testDispatcher)

        val sampleList = listOf(
            createSampleTlTimezone("Europe/London", "London", 0),
            createSampleTlTimezone("Europe/Paris", "Paris", 3600),
            createSampleTlTimezone("America/New_York", "New York", -18000)
        )
        local.setTestTimezones(sampleList)

        val timezones = repo.getTimezones()
        assertEquals(3, timezones.size)
        assertEquals("Europe/London", timezones[0].id)
        assertEquals("London", timezones[0].name)
        assertEquals("GMT", timezones[0].formattedOffset)
        assertEquals("Europe/Paris", timezones[1].id)
        assertEquals("GMT+01:00", timezones[1].formattedOffset)
        assertEquals("America/New_York", timezones[2].id)
        assertEquals("GMT-05:00", timezones[2].formattedOffset)
    }

    @Test
    fun testLoadTimezonesFromRemoteWhenCacheEmpty() = runTest(testDispatcher) {
        val local = TimezonesLocalDataSource(0)
        val remote = FakeTimezonesRemoteDataSource(0)
        val repo = TimezonesRepositoryImpl(0, local, remote, testDispatcher)

        remote.remoteTimezones = mutableListOf(
            createSampleTlTimezone("Asia/Tokyo", "Tokyo", 32400)
        )

        val result = repo.loadTimezones(forceReload = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Asia/Tokyo", list[0].id)
        assertEquals("GMT+09:00", list[0].formattedOffset)

        // Verify it was cached in local
        assertEquals(1, local.getTimezones().size)
    }

    @Test
    fun testFindTimezoneById() = runTest(testDispatcher) {
        val local = TimezonesLocalDataSource(0)
        val remote = FakeTimezonesRemoteDataSource(0)
        val repo = TimezonesRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestTimezones(listOf(
            createSampleTlTimezone("UTC", "Coordinated Universal Time", 0),
            createSampleTlTimezone("Europe/Berlin", "Berlin", 3600)
        ))

        val found = repo.findTimezone("Europe/Berlin")
        assertNotNull(found)
        assertEquals("Europe/Berlin", found?.id)
        assertEquals("Berlin", found?.name)
        assertEquals(3600, found?.utcOffsetSeconds)

        val notFound = repo.findTimezone("NonExistent")
        assertNull(notFound)
    }

    @Test
    fun testSystemTimezoneIdAndName() = runTest(testDispatcher) {
        val local = TimezonesLocalDataSource(0)
        val remote = FakeTimezonesRemoteDataSource(0)
        val repo = TimezonesRepositoryImpl(0, local, remote, testDispatcher)

        local.setTestSystemTimezoneId("Asia/Dubai")
        local.setTestTimezones(listOf(
            createSampleTlTimezone("Asia/Dubai", "Dubai", 14400)
        ))

        assertEquals("Asia/Dubai", repo.getSystemTimezoneId())
        assertEquals("Dubai", repo.getTimezoneName("Asia/Dubai", withOffset = false))
        assertEquals("Dubai, GMT+04:00", repo.getTimezoneName("Asia/Dubai", withOffset = true))
    }

    @Test
    fun testObserveTimezonesEmitsCurrent() = runTest(testDispatcher) {
        val local = TimezonesLocalDataSource(0)
        local.setTestTimezones(listOf(
            createSampleTlTimezone("Europe/Rome", "Rome", 3600)
        ))
        val remote = FakeTimezonesRemoteDataSource(0)
        val repo = TimezonesRepositoryImpl(0, local, remote, testDispatcher)

        val emitted = repo.observeTimezones().first()
        assertEquals(1, emitted.size)
        assertEquals("Europe/Rome", emitted[0].id)
    }
}
