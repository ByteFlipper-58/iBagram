package org.telegram.messenger.feature.system.animationlocker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.system.animationlocker.data.mapper.AnimationLockerMapper
import org.telegram.messenger.feature.system.animationlocker.data.repository.LegacyAnimationLockerRepository
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockEvaluator
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.AcquireAnimationLockUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerConfigUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerStateUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveAnimationLockerStateUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAllAnimationLocksUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAnimationLockUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.SetAnimationLockerDisabledUseCase
import org.telegram.messenger.feature.system.animationlocker.domain.usecase.UpdateAnimationLockerConfigUseCase
import org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerEvent
import org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AnimationLockerDomainTest {

    @Test
    fun testAnimationLockEvaluatorFilterAndNotificationAllowed() {
        // 1. Пустой список блокировок - любые уведомления разрешены
        assertTrue(AnimationLockEvaluator.isNotificationAllowed(100, emptyList()))

        // 2. Блокировка без белого списка - все уведомления заблокированы
        val strictLock = AnimationLockRecord(
            lockId = "l1",
            allowedNotificationIds = emptySet()
        )
        assertFalse(AnimationLockEvaluator.isNotificationAllowed(100, listOf(strictLock)))

        // 3. Блокировка с белым списком [100, 200]
        val selectiveLock1 = AnimationLockRecord(
            lockId = "l2",
            allowedNotificationIds = setOf(100, 200)
        )
        assertTrue(AnimationLockEvaluator.isNotificationAllowed(100, listOf(selectiveLock1)))
        assertTrue(AnimationLockEvaluator.isNotificationAllowed(200, listOf(selectiveLock1)))
        assertFalse(AnimationLockEvaluator.isNotificationAllowed(300, listOf(selectiveLock1)))

        // 4. Множественные одновременные блокировки (пересечение разрешений)
        val selectiveLock2 = AnimationLockRecord(
            lockId = "l3",
            allowedNotificationIds = setOf(200, 300)
        )
        val combinedLocks = listOf(selectiveLock1, selectiveLock2)
        // 200 есть в обоих списках -> разрешено
        assertTrue(AnimationLockEvaluator.isNotificationAllowed(200, combinedLocks))
        // 100 нет во втором локе -> отложено
        assertFalse(AnimationLockEvaluator.isNotificationAllowed(100, combinedLocks))
        // 300 нет в первом локе -> отложено
        assertFalse(AnimationLockEvaluator.isNotificationAllowed(300, combinedLocks))
    }

    @Test
    fun testAnimationLockEvaluatorTimeoutCheck() {
        val now = 100_000L
        val freshLock = AnimationLockRecord(
            lockId = "fresh",
            lockedAtMs = 95_000L // прошло 5 секунд
        )
        assertFalse(AnimationLockEvaluator.isLockExpired(freshLock, now, 10_000L))

        val expiredLock = AnimationLockRecord(
            lockId = "expired",
            lockedAtMs = 80_000L // прошло 20 секунд
        )
        assertTrue(AnimationLockEvaluator.isLockExpired(expiredLock, now, 10_000L))
    }

    @Test
    fun testMapperFormatting() {
        val idleState = AnimationLockerState(isLocked = false, currentAccount = 0)
        val idleSummary = AnimationLockerMapper.formatLockerSummary(idleState)
        assertTrue(idleSummary.contains("IDLE"))

        val lockedState = AnimationLockerState(
            isLocked = true,
            activeLocksCount = 1,
            activeLocks = listOf(
                AnimationLockRecord(
                    lockId = "lock_123",
                    tag = "DialogsTransition",
                    allowedNotificationIds = setOf(1, 2)
                )
            ),
            currentAccount = 1
        )
        val lockedSummary = AnimationLockerMapper.formatLockerSummary(lockedState)
        assertTrue(lockedSummary.contains("LOCKED"))
        assertTrue(lockedSummary.contains("DialogsTransition"))

        val record = AnimationLockRecord(
            lockId = "rec_1",
            tag = "PhotoOpen",
            scope = LockScope.ALL,
            allowedNotificationIds = setOf(42)
        )
        val recordStr = AnimationLockerMapper.formatLockRecord(record)
        assertTrue(recordStr.contains("PhotoOpen"))
        assertTrue(recordStr.contains("42"))
        assertTrue(recordStr.contains("ALL"))
    }

    @Test
    fun testRepositoryAcquireReleaseAndObserve() {
        val acquiredHandles = mutableListOf<Int>()
        val finishedHandles = mutableListOf<Int>()

        var handleSeed = 100
        val repo = LegacyAnimationLockerRepository(
            account = 0,
            onSetAnimationInProgress = { _, _, _, _ ->
                val h = ++handleSeed
                acquiredHandles.add(h)
                h
            },
            onAnimationFinish = { _, _, handle ->
                finishedHandles.add(handle)
            }
        )

        assertFalse(repo.isLocked())
        assertEquals(0, repo.getState().activeLocksCount)

        // Захватываем лок
        val lock1 = repo.acquireLock(tag = "test_tag", allowedNotificationIds = setOf(10, 20), scope = LockScope.ALL)
        assertTrue(repo.isLocked())
        assertEquals(1, repo.getState().activeLocksCount)
        assertEquals(2, acquiredHandles.size) // account + global handles
        assertTrue(repo.isNotificationAllowed(10))
        assertFalse(repo.isNotificationAllowed(99))

        // Освобождаем лок
        val released = repo.releaseLock(lock1.lockId)
        assertTrue(released)
        assertFalse(repo.isLocked())
        assertEquals(0, repo.getState().activeLocksCount)
        assertEquals(2, finishedHandles.size)
        assertTrue(repo.isNotificationAllowed(99))
    }

    @Test
    fun testRepositorySetDisabledAndReleaseAllLocks() {
        val repo = LegacyAnimationLockerRepository(account = 0)

        repo.acquireLock(tag = "lock_a")
        repo.acquireLock(tag = "lock_b")

        assertTrue(repo.isLocked())
        assertEquals(2, repo.getState().activeLocksCount)

        // Отключаем блокировщик
        repo.setDisabled(true)
        assertFalse(repo.isLocked())
        assertTrue(repo.getState().isDisabled)
        assertEquals(0, repo.getState().activeLocksCount)
        assertTrue(repo.isNotificationAllowed(777))

        // При disabled захват возвращает пустышку без удержания состояния
        val dummy = repo.acquireLock(tag = "lock_c")
        assertFalse(repo.isLocked())
        assertEquals(0, repo.getState().activeLocksCount)
        assertEquals("lock_c", dummy.tag)
    }

    @Test
    fun testAnimationLockerViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)

        val repo = LegacyAnimationLockerRepository(account = 0)

        val viewModel = AnimationLockerViewModel(
            acquireAnimationLockUseCase = AcquireAnimationLockUseCase(repo),
            releaseAnimationLockUseCase = ReleaseAnimationLockUseCase(repo),
            releaseAllAnimationLocksUseCase = ReleaseAllAnimationLocksUseCase(repo),
            setAnimationLockerDisabledUseCase = SetAnimationLockerDisabledUseCase(repo),
            getAnimationLockerStateUseCase = GetAnimationLockerStateUseCase(repo),
            getAnimationLockerConfigUseCase = GetAnimationLockerConfigUseCase(repo),
            updateAnimationLockerConfigUseCase = UpdateAnimationLockerConfigUseCase(repo),
            observeAnimationLockerStateUseCase = ObserveAnimationLockerStateUseCase(repo),
            scope = scope
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLocked)
        assertEquals(0, viewModel.uiState.value.activeLocksCount)

        // Захватываем лок через Event
        viewModel.onEvent(
            AnimationLockerEvent.AcquireLock(
                tag = "FragmentTransition",
                allowedNotificationIds = setOf(1, 2)
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLocked)
        assertEquals(1, viewModel.uiState.value.activeLocksCount)
        val activeLock = viewModel.uiState.value.activeLocks.first()
        assertEquals("FragmentTransition", activeLock.tag)

        // Освобождаем лок
        viewModel.onEvent(AnimationLockerEvent.ReleaseLock(activeLock.lockId))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLocked)
        assertEquals(0, viewModel.uiState.value.activeLocksCount)

        // Обновляем конфиг
        val newConfig = AnimationLockerConfig(safetyTimeoutMs = 5000L)
        viewModel.onEvent(AnimationLockerEvent.UpdateConfig(newConfig))
        advanceUntilIdle()

        assertEquals(5000L, viewModel.uiState.value.config.safetyTimeoutMs)

        // Отключаем
        viewModel.onEvent(AnimationLockerEvent.SetDisabled(true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDisabled)

        viewModel.onCleared()
    }
}
