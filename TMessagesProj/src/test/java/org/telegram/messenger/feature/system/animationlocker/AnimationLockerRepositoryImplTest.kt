package org.telegram.messenger.feature.system.animationlocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.animationlocker.data.datasource.AnimationLockerLocalDataSource
import org.telegram.messenger.feature.system.animationlocker.data.datasource.AnimationLockerRemoteDataSource
import org.telegram.messenger.feature.system.animationlocker.data.repository.AnimationLockerRepositoryImpl
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope

class AnimationLockerRepositoryImplTest {

    private lateinit var localDataSource: AnimationLockerLocalDataSource
    private lateinit var remoteDataSource: AnimationLockerRemoteDataSource
    private lateinit var repository: AnimationLockerRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AnimationLockerLocalDataSource(account = 0).apply {
            setTestMode(true)
        }
        remoteDataSource = AnimationLockerRemoteDataSource(currentAccount = 0)
        repository = AnimationLockerRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialState_isNotLockedAndEmpty() {
        val state = repository.getState()
        assertFalse(repository.isLocked())
        assertFalse(state.isLocked)
        assertEquals(0, state.activeLocksCount)
        assertTrue(state.activeLocks.isEmpty())
        assertFalse(state.isDisabled)
    }

    @Test
    fun acquireLock_locksAndGeneratesValidRecord() {
        val record = repository.acquireLock(
            tag = "chat_open",
            allowedNotificationIds = setOf(10, 20),
            scope = LockScope.ALL
        )

        assertNotNull(record.lockId)
        assertEquals("chat_open", record.tag)
        assertEquals(setOf(10, 20), record.allowedNotificationIds)
        assertEquals(LockScope.ALL, record.scope)

        assertTrue(repository.isLocked())
        val state = repository.getState()
        assertTrue(state.isLocked)
        assertEquals(1, state.activeLocksCount)
        assertEquals(1L, state.totalAcquiredLocks)
    }

    @Test
    fun releaseLock_removesLockAndUnlocksWhenEmpty() {
        val record = repository.acquireLock(tag = "profile_anim")
        assertTrue(repository.isLocked())

        val released = repository.releaseLock(record.lockId)
        assertTrue(released)

        assertFalse(repository.isLocked())
        val state = repository.getState()
        assertEquals(0, state.activeLocksCount)
        assertEquals(1L, state.totalReleasedLocks)
    }

    @Test
    fun releaseAllLocks_clearsAllActiveLocks() {
        val lock1 = repository.acquireLock(tag = "lock_1")
        val lock2 = repository.acquireLock(tag = "lock_2")
        assertEquals(2, repository.getState().activeLocksCount)

        repository.releaseAllLocks()
        assertFalse(repository.isLocked())
        assertEquals(0, repository.getState().activeLocksCount)
    }

    @Test
    fun setDisabled_ignoresLocksWhenDisabled() {
        repository.setDisabled(true)
        assertTrue(repository.getState().isDisabled)

        val record = repository.acquireLock(tag = "disabled_lock")
        assertFalse(repository.isLocked())
        assertEquals(0, repository.getState().activeLocksCount)

        repository.setDisabled(false)
        assertFalse(repository.getState().isDisabled)
    }

    @Test
    fun isNotificationAllowed_filtersBasedOnAllowedIds() {
        // Without locks, everything is allowed
        assertTrue(repository.isNotificationAllowed(99))

        // Lock allowing only notification id 42
        val lock = repository.acquireLock(
            tag = "filtered_anim",
            allowedNotificationIds = setOf(42)
        )

        assertTrue(repository.isNotificationAllowed(42))
        assertFalse(repository.isNotificationAllowed(99))

        repository.releaseLock(lock.lockId)
        assertTrue(repository.isNotificationAllowed(99))
    }

    @Test
    fun observeState_emitsUpdatedStateOnAcquireAndRelease() {
        val stateFlow = repository.observeState()
        val isLockedFlow = repository.observeIsLocked()

        assertFalse(isLockedFlow.value)
        assertEquals(0, stateFlow.value.activeLocksCount)

        val lock = repository.acquireLock(tag = "flow_anim")
        assertTrue(isLockedFlow.value)
        assertEquals(1, stateFlow.value.activeLocksCount)

        repository.releaseLock(lock.lockId)
        assertFalse(isLockedFlow.value)
        assertEquals(0, stateFlow.value.activeLocksCount)
    }
}
