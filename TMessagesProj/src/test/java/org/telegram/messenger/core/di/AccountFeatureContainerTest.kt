package org.telegram.messenger.core.di

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class AccountFeatureContainerTest {

    @Before
    fun setUp() {
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
    }

    @Test
    fun testAccountIsolation() {
        val container0 = AccountFeatureContainer.get(0)
        val container1 = AccountFeatureContainer.get(1)

        assertEquals(0, container0.account)
        assertEquals(1, container1.account)
        assertNotSame(container0, container1)

        // Same account returns same cached instance
        val container0Again = AccountFeatureContainer.get(0)
        assertSame(container0, container0Again)
    }

    @Test
    fun testResetAccount() {
        val container0 = AccountFeatureContainer.get(0)
        AccountFeatureContainer.reset(0)
        val container0New = AccountFeatureContainer.get(0)
        assertNotSame(container0, container0New)
    }

    @Test
    fun testResetAll() {
        val container0 = AccountFeatureContainer.get(0)
        val container1 = AccountFeatureContainer.get(1)
        AccountFeatureContainer.resetAll()

        assertNotSame(container0, AccountFeatureContainer.get(0))
        assertNotSame(container1, AccountFeatureContainer.get(1))
    }
}
