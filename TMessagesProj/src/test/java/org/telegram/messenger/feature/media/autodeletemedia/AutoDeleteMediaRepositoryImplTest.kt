package org.telegram.messenger.feature.media.autodeletemedia

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.autodeletemedia.data.datasource.AutoDeleteMediaLocalDataSource
import org.telegram.messenger.feature.media.autodeletemedia.data.datasource.AutoDeleteMediaRemoteDataSource
import org.telegram.messenger.feature.media.autodeletemedia.data.repository.AutoDeleteMediaRepositoryImpl

class AutoDeleteMediaRepositoryImplTest {

    private lateinit var localDataSource: AutoDeleteMediaLocalDataSource
    private lateinit var remoteDataSource: AutoDeleteMediaRemoteDataSource
    private lateinit var repository: AutoDeleteMediaRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AutoDeleteMediaLocalDataSource(currentAccount = 0, testMode = true)
        remoteDataSource = AutoDeleteMediaRemoteDataSource(currentAccount = 0)
        repository = AutoDeleteMediaRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() = runTest {
        val state = repository.getState()
        assertFalse(state.isRunning)
        assertEquals(0, state.lockedFilesCount)

        val flowState = repository.observeState().first()
        assertFalse(flowState.isRunning)
    }

    @Test
    fun testFileLockingAndUnlocking() = runTest {
        val testPath = "/sdcard/telegram/audio.mp3"
        assertFalse(repository.isFileLocked(testPath))

        repository.lockFile(testPath)
        assertTrue(repository.isFileLocked(testPath))
        assertEquals(1, repository.getState().lockedFilesCount)

        repository.unlockFile(testPath)
        assertFalse(repository.isFileLocked(testPath))
        assertEquals(0, repository.getState().lockedFilesCount)

        repository.lockFile("/path/one")
        repository.lockFile("/path/two")
        assertEquals(2, repository.getState().lockedFilesCount)

        repository.clearLockedFiles()
        assertEquals(0, repository.getState().lockedFilesCount)
    }

    @Test
    fun testRunCleanup() = runTest {
        val result = repository.runCleanup(force = true)
        assertTrue(result.isSuccess)
        val runResult = result.getOrNull()
        assertNotNull(runResult)
        assertEquals(10, runResult?.filesScanned)
        assertEquals(2, runResult?.autoDeletedFiles)
        assertFalse(repository.getState().isRunning)
        assertNotNull(repository.getState().lastResult)
    }

    @Test
    fun testRemoteDataSource() = runTest {
        val policyRes = remoteDataSource.fetchMediaRetentionPolicy()
        assertTrue(policyRes.isSuccess)
    }
}
