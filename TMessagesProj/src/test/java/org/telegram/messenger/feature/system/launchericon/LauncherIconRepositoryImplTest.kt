package org.telegram.messenger.feature.system.launchericon

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.data.datasource.LauncherIconLocalDataSource
import org.telegram.messenger.feature.system.launchericon.data.datasource.LauncherIconRemoteDataSource
import org.telegram.messenger.feature.system.launchericon.data.repository.LauncherIconRepositoryImpl
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherIconRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: LauncherIconLocalDataSource
    private lateinit var remoteDataSource: LauncherIconRemoteDataSource
    private lateinit var repository: LauncherIconRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = LauncherIconLocalDataSource(currentAccount = 0).apply {
            setTestMode(LauncherIconType.DEFAULT)
        }
        remoteDataSource = LauncherIconRemoteDataSource(currentAccount = 0)
        repository = LauncherIconRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @Test
    fun testGetLauncherIcons() {
        val icons = repository.getLauncherIcons()
        assertEquals(6, icons.size)
        val defaultIcon = icons.firstOrNull { it.type == LauncherIconType.DEFAULT }
        assertNotNull(defaultIcon)
        assertTrue(defaultIcon!!.isEnabled)
        assertFalse(defaultIcon.isPremium)

        val premiumIcon = icons.firstOrNull { it.type == LauncherIconType.PREMIUM }
        assertNotNull(premiumIcon)
        assertFalse(premiumIcon!!.isEnabled)
        assertTrue(premiumIcon.isPremium)
    }

    @Test
    fun testGetActiveIcon() {
        val active = repository.getActiveIcon()
        assertNotNull(active)
        assertEquals(LauncherIconType.DEFAULT, active!!.type)
        assertTrue(active.isEnabled)
    }

    @Test
    fun testIsIconEnabled() {
        assertTrue(repository.isIconEnabled(LauncherIconType.DEFAULT))
        assertFalse(repository.isIconEnabled(LauncherIconType.TURBO))
        assertFalse(repository.isIconEnabled(LauncherIconType.NOX))
    }

    @Test
    fun testSetIcon() = runTest(testDispatcher) {
        val result = repository.setIcon(LauncherIconType.TURBO)
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)

        assertTrue(repository.isIconEnabled(LauncherIconType.TURBO))
        assertFalse(repository.isIconEnabled(LauncherIconType.DEFAULT))

        val active = repository.getActiveIcon()
        assertNotNull(active)
        assertEquals(LauncherIconType.TURBO, active!!.type)
    }

    @Test
    fun testFixLauncherIconIfNeeded() = runTest(testDispatcher) {
        val result = repository.fixLauncherIconIfNeeded()
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
        assertNotNull(repository.getActiveIcon())
    }

    @Test
    fun testObserveLauncherIcons() = runTest(testDispatcher) {
        val initialState = repository.observeLauncherIcons().first()
        assertEquals(LauncherIconType.DEFAULT, initialState.activeIcon?.type)

        repository.setIcon(LauncherIconType.AQUA)
        val updatedState = repository.observeLauncherIcons().first()
        assertEquals(LauncherIconType.AQUA, updatedState.activeIcon?.type)
    }
}
