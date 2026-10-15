package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.repository.testing.FakeLocalDataSource
import com.nghianguyen.gomuni.data.repository.testing.FakeRemoteDataSource
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransitRepositoryImplTest {
    @Test
    fun `cancelled data operations propagate without logging or clearing cache`() = runTest {
        val cancelled = CancellationException("Request cancelled")
        val remote = FakeRemoteDataSource().apply { failure = cancelled }
        val local = FakeLocalDataSource()
        val logger = RecordingErrorLogger()
        val repository = TransitRepositoryImpl(remote, local, logger)
        val operations: List<suspend () -> Unit> =
            listOf(
                { repository.refreshTransitLines("SF", forceRefresh = true) },
                { repository.refreshTransitStops("SF", "19") },
                { repository.getBusArrivals("123") },
            )

        operations.forEach { operation ->
            try {
                operation()
                fail("Cancellation must propagate")
            } catch (actual: CancellationException) {
                assertSame(cancelled, actual)
            }
        }
        assertTrue(logger.errors.isEmpty())
        assertFalse(local.cleared)
    }

    @Test
    fun `arrival network failure is logged and mapped to domain failure`() = runTest {
        val failure = IOException("Offline")
        val remote = FakeRemoteDataSource().apply { this.failure = failure }
        val logger = RecordingErrorLogger()
        val repository = TransitRepositoryImpl(remote, FakeLocalDataSource(), logger)

        try {
            repository.getBusArrivals("123")
            fail("Network failure must propagate")
        } catch (actual: TransitError) {
            assertSame(TransitError.Network, actual)
        }
        assertSame(failure, logger.errors.single())
    }
}
