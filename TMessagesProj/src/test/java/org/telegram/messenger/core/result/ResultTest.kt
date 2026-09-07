package org.telegram.messenger.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun testSuccessOperations() {
        val success = Result.success("telegram")
        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)
        assertEquals("telegram", success.getOrNull())
        assertEquals("telegram", success.getOrDefault("fallback"))

        var transformed = ""
        val mapped = success.map { it.uppercase() }
        mapped.onSuccess { transformed = it }
        assertEquals("TELEGRAM", transformed)

        val flatMapped = success.flatMap { Result.success(it.length) }
        assertEquals(8, flatMapped.getOrNull())
    }

    @Test
    fun testFailureOperations() {
        val error = AppError.Network("Connection lost", 503)
        val failure: Result<String> = Result.failure(error)

        assertFalse(failure.isSuccess)
        assertTrue(failure.isFailure)
        assertNull(failure.getOrNull())
        assertEquals("default", failure.getOrDefault("default"))

        var handledError: AppError? = null
        failure.onFailure { handledError = it }
        assertEquals(error, handledError)

        val mapped = failure.map { it.uppercase() }
        assertTrue(mapped.isFailure)
    }
}
