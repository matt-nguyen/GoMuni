package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.repository.testing.FakeLocalDataSource
import com.nghianguyen.gomuni.data.repository.testing.FakeRemoteDataSource
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
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
        val local = FakeLocalDataSource().apply { lines.value = emptyList() }
        val logger = RecordingErrorLogger()
        val repository = TransitRepositoryImpl(remote, local, logger)
        val operations: List<suspend () -> Unit> =
            listOf(
                { repository.ensureTransitLinesLoaded("SF") },
                { repository.ensureTransitStopsLoaded("SF", "19") },
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

    @Test
    fun `empty datasets are fetched again across repository recreation`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource().apply { lineResult = emptyList() }
        val first = TransitRepositoryImpl(remote, local, RecordingErrorLogger())
        local.lines.value = emptyList()
        first.ensureTransitStopsLoaded("SF", "19")
        val recreated = TransitRepositoryImpl(remote, local, RecordingErrorLogger())
        recreated.ensureTransitLinesLoaded("SF")
        recreated.ensureTransitStopsLoaded("SF", "19")
        org.junit.Assert.assertEquals(3, remote.lineCalls)
        org.junit.Assert.assertEquals(2, remote.stopCalls)
    }

    @Test
    fun `failed initial load leaves cache empty and retry succeeds`() = runTest {
        val local = FakeLocalDataSource()
        local.lines.value = emptyList()
        val remote = FakeRemoteDataSource().apply { failure = IOException("Offline") }
        val repository = TransitRepositoryImpl(remote, local, RecordingErrorLogger())
        try {
            repository.ensureTransitLinesLoaded("SF")
            fail("Expected failure")
        } catch (_: TransitError) {}
        assertTrue(local.lines.value.isEmpty())
        remote.failure = null
        repository.ensureTransitLinesLoaded("SF")
        org.junit.Assert.assertEquals(2, remote.lineCalls)
        assertTrue(local.lines.value.isNotEmpty())
    }

    @Test
    fun `concurrent initial requests fetch a dataset once`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val remote = FakeRemoteDataSource().apply { beforeLines = { gate.await() } }
        val repository =
            TransitRepositoryImpl(
                remote,
                FakeLocalDataSource().apply { lines.value = emptyList() },
                RecordingErrorLogger(),
            )
        val first = async { repository.ensureTransitLinesLoaded("SF") }
        val second = async { repository.ensureTransitLinesLoaded("SF") }
        gate.complete(Unit)
        first.await()
        second.await()
        org.junit.Assert.assertEquals(1, remote.lineCalls)
    }
}
