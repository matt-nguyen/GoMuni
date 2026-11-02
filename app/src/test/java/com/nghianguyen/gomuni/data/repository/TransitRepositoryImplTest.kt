package com.nghianguyen.gomuni.data.repository

import com.nghianguyen.gomuni.data.repository.testing.FakeLocalDataSource
import com.nghianguyen.gomuni.data.repository.testing.FakeRemoteDataSource
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitError
import com.nghianguyen.gomuni.testing.RecordingErrorLogger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class TransitRepositoryImplTest {
    @Test
    fun `HTTP failures return typed missing or unavailable service errors`() = runTest {
        mapOf(404 to TransitError.NotFound, 503 to TransitError.ServiceUnavailable).forEach {
            (code, error) ->
            val remote =
                FakeRemoteDataSource().apply {
                    failure =
                        HttpException(Response.error<Unit>(code, "Unavailable".toResponseBody()))
                }
            val repository =
                TransitRepositoryImpl(remote, FakeLocalDataSource(), RecordingErrorLogger())
            assertEquals(Outcome.Failure(error), repository.getBusArrivals("123"))
        }
    }

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

        assertEquals(Outcome.Failure(TransitError.Network), repository.getBusArrivals("123"))
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
        assertEquals(3, remote.lineCalls)
        assertEquals(2, remote.stopCalls)
    }

    @Test
    fun `failed initial load leaves cache empty and retry succeeds`() = runTest {
        val local = FakeLocalDataSource()
        local.lines.value = emptyList()
        val remote = FakeRemoteDataSource().apply { failure = IOException("Offline") }
        val repository = TransitRepositoryImpl(remote, local, RecordingErrorLogger())
        assertEquals(
            Outcome.Failure(TransitError.Network),
            repository.ensureTransitLinesLoaded("SF"),
        )
        assertTrue(local.lines.value.isEmpty())
        remote.failure = null
        repository.ensureTransitLinesLoaded("SF")
        assertEquals(2, remote.lineCalls)
        assertTrue(local.lines.value.isNotEmpty())
    }

    @Test
    fun `concurrent initial requests fetch a dataset once`() = runTest {
        val gate = CompletableDeferred<Unit>()
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
        assertEquals(1, remote.lineCalls)
    }

    @Test
    fun `local read failure is storage failure and never fetches remote data`() = runTest {
        val local = FakeLocalDataSource().apply { readFailure = IOException("Disk unavailable") }
        val remote = FakeRemoteDataSource()
        val logger = RecordingErrorLogger()
        val repository = TransitRepositoryImpl(remote, local, logger)

        assertEquals(Outcome.Failure(TransitError.Storage), repository.getTransitLines().first())
        assertEquals(
            Outcome.Failure(TransitError.Storage),
            repository.ensureTransitLinesLoaded("SF"),
        )
        assertEquals(0, remote.lineCalls)
        assertEquals(2, logger.errors.size)
    }

    @Test
    fun `local write failure is storage failure and can retry`() = runTest {
        val local =
            FakeLocalDataSource().apply {
                lines.value = emptyList()
                writeFailure = IOException("Disk full")
            }
        val repository =
            TransitRepositoryImpl(FakeRemoteDataSource(), local, RecordingErrorLogger())

        assertEquals(
            Outcome.Failure(TransitError.Storage),
            repository.ensureTransitLinesLoaded("SF"),
        )
        assertTrue(local.lines.value.isEmpty())
        local.writeFailure = null
        assertEquals(Outcome.Success(Unit), repository.ensureTransitLinesLoaded("SF"))
        assertTrue(local.lines.value.isNotEmpty())
    }

    @Test
    fun `unexpected failures remain exceptions`() = runTest {
        val error = IllegalStateException("Broken source")
        val remote = FakeRemoteDataSource().apply { failure = error }
        val repository =
            TransitRepositoryImpl(remote, FakeLocalDataSource(), RecordingErrorLogger())
        try {
            repository.getBusArrivals("123")
            fail("Expected unexpected failure")
        } catch (actual: IllegalStateException) {
            assertSame(error, actual)
        }
    }
}
