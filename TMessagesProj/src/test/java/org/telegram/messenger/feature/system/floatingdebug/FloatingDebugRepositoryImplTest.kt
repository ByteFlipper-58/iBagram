package org.telegram.messenger.feature.system.floatingdebug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.floatingdebug.data.datasource.FloatingDebugLocalDataSource
import org.telegram.messenger.feature.system.floatingdebug.data.datasource.FloatingDebugRemoteDataSource
import org.telegram.messenger.feature.system.floatingdebug.data.repository.FloatingDebugRepositoryImpl
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemKind
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel

class FloatingDebugRepositoryImplTest {

    private lateinit var localDataSource: FloatingDebugLocalDataSource
    private lateinit var remoteDataSource: FloatingDebugRemoteDataSource
    private lateinit var repository: FloatingDebugRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = FloatingDebugLocalDataSource().apply {
            setTestMode(true)
        }
        remoteDataSource = FloatingDebugRemoteDataSource(currentAccount = 0)
        repository = FloatingDebugRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialState_isNotActiveAndEmpty() {
        assertFalse(repository.isActive())
        val state = repository.getState()
        assertFalse(state.isActive)
        assertTrue(state.items.isEmpty())
        assertEquals(0, state.itemsCount)
        assertTrue(repository.getDebugItems().isEmpty())
    }

    @Test
    fun setActive_updatesActiveState() {
        repository.setActive(true, saveConfig = false)
        assertTrue(repository.isActive())
        assertTrue(repository.getState().isActive)

        repository.setActive(false, saveConfig = false)
        assertFalse(repository.isActive())
        assertFalse(repository.getState().isActive)
    }

    @Test
    fun toggleActive_togglesBetweenTrueAndFalse() {
        assertFalse(repository.isActive())

        val nowActive = repository.toggleActive(saveConfig = false)
        assertTrue(nowActive)
        assertTrue(repository.isActive())

        val nowInactive = repository.toggleActive(saveConfig = false)
        assertFalse(nowInactive)
        assertFalse(repository.isActive())
    }

    @Test
    fun registerDebugItems_addsItemsToRepository() {
        val item1 = DebugItemModel(title = "Show FPS", kind = DebugItemKind.SIMPLE)
        val item2 = DebugItemModel(title = "Rendering Section", kind = DebugItemKind.HEADER)
        val item3 = DebugItemModel(title = "Animation Speed", kind = DebugItemKind.SEEKBAR, from = 0.5f, to = 2.0f, currentValue = 1.0f)

        repository.registerDebugItems(listOf(item1, item2, item3))

        assertEquals(3, repository.getDebugItems().size)
        val state = repository.getState()
        assertEquals(3, state.itemsCount)
        assertEquals("Show FPS", state.items[0].title)
        assertEquals(DebugItemKind.HEADER, state.items[1].kind)
        assertEquals(1.0f, state.items[2].currentValue, 0.001f)
    }

    @Test
    fun clearDebugItems_emptiesRegisteredItems() {
        val item = DebugItemModel(title = "Item 1", kind = DebugItemKind.SIMPLE)
        repository.registerDebugItems(listOf(item))
        assertEquals(1, repository.getDebugItems().size)

        repository.clearDebugItems()
        assertTrue(repository.getDebugItems().isEmpty())
        assertEquals(0, repository.getState().itemsCount)
    }

    @Test
    fun getState_reflectsCurrentStateAccurately() {
        repository.setActive(true, saveConfig = false)
        val item = DebugItemModel(title = "Active Tool", kind = DebugItemKind.SIMPLE)
        repository.registerDebugItems(listOf(item))

        val state = repository.getState()
        assertTrue(state.isActive)
        assertEquals(1, state.itemsCount)
        assertEquals("Active Tool", state.items[0].title)
    }
}
