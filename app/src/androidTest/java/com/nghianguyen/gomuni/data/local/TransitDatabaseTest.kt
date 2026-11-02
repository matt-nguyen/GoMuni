package com.nghianguyen.gomuni.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSourceImpl
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.data.repository.TransitRepositoryImpl
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.Outcome
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Verifies real Room transactions, shared associations, rollback, and reopening. */
class TransitDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun initialLoads_whenReopened_preserveDataAndRollbackFailedStops() = runBlocking {
        val name = "initial-load-${UUID.randomUUID()}"
        try {
            Room.databaseBuilder(context, TransitDatabase::class.java, name).build().useDatabase {
                database ->
                val local =
                    TransitLocalDataSourceImpl(database.transitLineDao(), database.transitStopDao())
                local.storeInitialLines("SF", listOf(TransitLine("38", "Geary", "bus")))
                local.storeInitialStops("SF", "38", emptyList())
                try {
                    local.storeInitialStops(
                        "SF",
                        "missing",
                        listOf(TransitStop("bad", "Bad", 0.0, 0.0, null, "missing")),
                    )
                    fail("Expected foreign key failure")
                } catch (_: SQLiteConstraintException) {
                    assertTrue(local.getStopsForLine("missing").first().isEmpty())
                    database
                        .query("SELECT COUNT(*) FROM transit_stops WHERE id = 'bad'", emptyArray())
                        .use { cursor ->
                            assertTrue(cursor.moveToFirst())
                            assertEquals(0, cursor.getInt(0))
                        }
                }
            }
            Room.databaseBuilder(context, TransitDatabase::class.java, name).build().useDatabase {
                database ->
                val local =
                    TransitLocalDataSourceImpl(database.transitLineDao(), database.transitStopDao())
                assertEquals("38", local.getTransitLines().first().single().id)
                assertTrue(local.getStopsForLine("38").first().isEmpty())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun overlappingRoutes_whenReopened_preserveBothCompletedDatasetsWithoutRefetching() =
        runBlocking {
            val name = "overlapping-routes-${UUID.randomUUID()}"
            val lineCalls = mutableListOf<String>()
            val stopCalls = mutableListOf<String>()
            val remote =
                object : TransitRemoteDataSource {
                    override suspend fun getTransitLines(operatorId: String): List<TransitLine> {
                        lineCalls.add(operatorId)
                        return listOf(
                            TransitLine("38", "Geary", "bus"),
                            TransitLine("38R", "Geary Rapid", "bus"),
                        )
                    }

                    override suspend fun getTransitStops(
                        operatorId: String,
                        lineId: String,
                    ): List<TransitStop> {
                        stopCalls.add(lineId)
                        return listOf(
                            TransitStop("123", "Shared stop", 37.78, -122.41, null, lineId),
                        )
                    }

                    override suspend fun getBusArrivals(
                        stopId: String,
                    ): List<BusArrival> = emptyList()
                }
            val logger =
                object : ErrorLogger {
                    override fun log(
                        message: String,
                        error: Throwable?,
                        metadata: Map<String, String>,
                    ) {
                        throw AssertionError(message, error)
                    }
                }
            try {
                Room.databaseBuilder(context, TransitDatabase::class.java, name)
                    .build()
                    .useDatabase { database ->
                        val local =
                            TransitLocalDataSourceImpl(
                                database.transitLineDao(),
                                database.transitStopDao(),
                            )
                        val repository =
                            TransitRepositoryImpl(
                                remote,
                                local,
                                logger,
                            )
                        assertEquals(
                            Outcome.Success(Unit),
                            repository.ensureTransitStopsLoaded("SF", "38"),
                        )
                        assertEquals(
                            Outcome.Success(Unit),
                            repository.ensureTransitStopsLoaded("SF", "38R"),
                        )
                        assertEquals(
                            "123",
                            (repository.getTransitStops("38").first() as Outcome.Success)
                                .value
                                .single()
                                .id,
                        )
                        assertEquals(
                            "123",
                            (repository.getTransitStops("38R").first() as Outcome.Success)
                                .value
                                .single()
                                .id,
                        )
                        local.storeInitialStops(
                            "SF",
                            "38R",
                            listOf(TransitStop("123", "Updated", 37.78, -122.41, null, "38")),
                        )
                        assertEquals("Updated", local.getStopsForLine("38").first().single().name)
                        assertEquals("38R", local.getStopsForLine("38R").first().single().lineId)
                        database.query("SELECT COUNT(*) FROM transit_stops", emptyArray()).use {
                            cursor ->
                            assertTrue(cursor.moveToFirst())
                            assertEquals(1, cursor.getInt(0))
                        }
                    }
                Room.databaseBuilder(context, TransitDatabase::class.java, name)
                    .build()
                    .useDatabase { database ->
                        val local =
                            TransitLocalDataSourceImpl(
                                database.transitLineDao(),
                                database.transitStopDao(),
                            )
                        val repository =
                            TransitRepositoryImpl(
                                remote,
                                local,
                                logger,
                            )
                        assertEquals(
                            Outcome.Success(Unit),
                            repository.ensureTransitStopsLoaded("SF", "38"),
                        )
                        assertEquals(
                            Outcome.Success(Unit),
                            repository.ensureTransitStopsLoaded("SF", "38R"),
                        )
                        assertEquals(
                            "38",
                            (repository.getTransitStops("38").first() as Outcome.Success)
                                .value
                                .single()
                                .lineId,
                        )
                        assertEquals(
                            "38R",
                            (repository.getTransitStops("38R").first() as Outcome.Success)
                                .value
                                .single()
                                .lineId,
                        )
                    }
                assertEquals(listOf("SF"), lineCalls)
                assertEquals(listOf("38", "38R"), stopCalls)
            } finally {
                context.deleteDatabase(name)
            }
        }
}

private inline fun <T> TransitDatabase.useDatabase(block: (TransitDatabase) -> T): T {
    try {
        return block(this)
    } finally {
        close()
    }
}
