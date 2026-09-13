package org.telegram.messenger.feature.system.windowvisibility

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.windowvisibility.data.datasource.WindowVisibilityLocalDataSource
import org.telegram.messenger.feature.system.windowvisibility.data.datasource.WindowVisibilityRemoteDataSource
import org.telegram.messenger.feature.system.windowvisibility.data.repository.WindowVisibilityRepositoryImpl

class WindowVisibilityRepositoryImplTest {

    private lateinit var localDataSource: WindowVisibilityLocalDataSource
    private lateinit var remoteDataSource: WindowVisibilityRemoteDataSource
    private lateinit var repository: WindowVisibilityRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = WindowVisibilityLocalDataSource(currentAccount = 0)
        remoteDataSource = WindowVisibilityRemoteDataSource(currentAccount = 0)
        repository = WindowVisibilityRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun testDefaultState() {
        assertTrue(repository.isVisible())
        assertFalse(repository.isHidden())
        assertEquals(0, repository.getReasonsCount())
        assertTrue(repository.getActiveReasons().isEmpty())

        val state = repository.getCurrentState()
        assertTrue(state.isVisible)
        assertEquals(0, state.reasonsCount)
    }

    @Test
    fun testRequestHideAndRelease() {
        val stateAfterHide = repository.requestHide("dialog_open", "Showing test dialog")
        assertFalse(repository.isVisible())
        assertTrue(repository.isHidden())
        assertEquals(1, repository.getReasonsCount())
        assertTrue(repository.getActiveReasons().contains("dialog_open"))
        assertEquals("dialog_open", stateAfterHide.lastChangedReason)

        val stateAfterRelease = repository.releaseHide("dialog_open")
        assertTrue(repository.isVisible())
        assertFalse(repository.isHidden())
        assertEquals(0, repository.getReasonsCount())
        assertFalse(repository.getActiveReasons().contains("dialog_open"))
    }

    @Test
    fun testMultipleReasonsArbitration() {
        repository.requestHide("reason_1")
        repository.requestHide("reason_2")
        assertEquals(2, repository.getReasonsCount())
        assertTrue(repository.isHidden())

        repository.releaseHide("reason_1")
        assertEquals(1, repository.getReasonsCount())
        assertTrue(repository.isHidden())

        repository.releaseHide("reason_2")
        assertEquals(0, repository.getReasonsCount())
        assertTrue(repository.isVisible())
    }

    @Test
    fun testToggleHide() {
        repository.toggleHide("toggle_test", true)
        assertTrue(repository.isHidden())

        repository.toggleHide("toggle_test", false)
        assertTrue(repository.isVisible())
    }

    @Test
    fun testResetAllReasons() {
        repository.requestHide("r1")
        repository.requestHide("r2")
        repository.requestHide("r3")
        assertEquals(3, repository.getReasonsCount())

        val resetState = repository.resetAllReasons()
        assertTrue(resetState.isVisible)
        assertEquals(0, repository.getReasonsCount())
        assertTrue(repository.getActiveReasons().isEmpty())
        assertTrue(repository.isVisible())
    }

    @Test
    fun testObtainControllerLifecycle() {
        val controller = repository.obtainController("subsystem_tag")
        assertEquals("subsystem_tag", controller.reasonTag)
        assertFalse(controller.isHidden)
        assertFalse(controller.isDestroyed)

        controller.setHidden(true)
        assertTrue(controller.isHidden)
        assertTrue(repository.isHidden())

        controller.setHidden(false)
        assertFalse(controller.isHidden)
        assertTrue(repository.isVisible())

        controller.setHidden(true)
        assertTrue(repository.isHidden())
        controller.destroy()
        assertTrue(controller.isDestroyed)
        assertTrue(repository.isVisible())
    }

    @Test
    fun testObserveStateAndVisibility() = runTest {
        val initialState = repository.observeState().first()
        assertTrue(initialState.isVisible)

        val initialVisibility = repository.observeVisibilityChanges().first()
        assertTrue(initialVisibility)

        repository.requestHide("flow_test")
        val updatedState = repository.observeState().first()
        assertFalse(updatedState.isVisible)
        assertEquals(1, updatedState.reasonsCount)
    }
}
