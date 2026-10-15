package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.repository.testing.FakeLocalDataSource
import com.nghianguyen.gomuni.data.repository.testing.FakeRemoteDataSource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class TransitRepositoryTest {
    @Test
    fun `cancelled refresh propagates even when cached data exists`() = runTest {
        val cancelled = CancellationException("Refresh cancelled")
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource().apply { failure = cancelled }
        val repository = TransitRepository(remote, local)

        try {
            repository.refreshTransitLines("SF", forceRefresh = true)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
        assertFalse(local.cleared)
        assertEquals("19", local.lines.value.single().id)
    }

    @Test
    fun `network failure retains an existing cache`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource().apply { failure = IOException("Offline") }
        val repository = TransitRepository(remote, local)

        repository.refreshTransitLines("SF", forceRefresh = true)

        assertFalse(local.cleared)
        assertEquals("19", local.lines.value.single().id)
    }
}
