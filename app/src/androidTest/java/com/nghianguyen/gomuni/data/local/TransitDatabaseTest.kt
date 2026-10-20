package com.nghianguyen.gomuni.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSourceImpl
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Verifies real Room transactions, completion markers, reopening, and the v2 migration. */
class TransitDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun initialLoads_whenReopened_preserveDataAndEmptyCompletionMarkers() = runBlocking {
        val name = "initial-load-${UUID.randomUUID()}"
        try {
            Room.databaseBuilder(context, TransitDatabase::class.java, name).build().useDatabase {
                database ->
                val local = TransitLocalDataSourceImpl(database)
                local.storeInitialLines("SF", listOf(TransitLine("38", "Geary", "bus")))
                local.storeInitialStops("SF", "38", emptyList())
                try {
                    local.storeInitialStops(
                        "SF",
                        "missing",
                        listOf(TransitStop("bad", "Bad", 0.0, 0.0, null, "missing")),
                    )
                    fail("Expected foreign key failure")
                } catch (_: android.database.sqlite.SQLiteConstraintException) {
                    assertFalse(local.isLoaded("stops:SF:missing"))
                }
            }
            Room.databaseBuilder(context, TransitDatabase::class.java, name).build().useDatabase {
                database ->
                val local = TransitLocalDataSourceImpl(database)
                assertEquals("38", local.getTransitLines().first().single().id)
                assertTrue(local.isLoaded("lines:SF"))
                assertTrue(local.isLoaded("stops:SF:38"))
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
                object : com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource {
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
                    ): List<com.nghianguyen.gomuni.domain.model.BusArrival> = emptyList()
                }
            val logger =
                object : com.nghianguyen.gomuni.domain.util.ErrorLogger {
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
                        val local = TransitLocalDataSourceImpl(database)
                        val repository =
                            com.nghianguyen.gomuni.data.repository.TransitRepositoryImpl(
                                remote,
                                local,
                                logger,
                            )
                        repository.ensureTransitStopsLoaded("SF", "38")
                        repository.ensureTransitStopsLoaded("SF", "38R")
                        assertEquals("123", repository.getTransitStops("38").first().single().id)
                        assertEquals("123", repository.getTransitStops("38R").first().single().id)
                    }
                Room.databaseBuilder(context, TransitDatabase::class.java, name)
                    .build()
                    .useDatabase { database ->
                        val local = TransitLocalDataSourceImpl(database)
                        val repository =
                            com.nghianguyen.gomuni.data.repository.TransitRepositoryImpl(
                                remote,
                                local,
                                logger,
                            )
                        repository.ensureTransitStopsLoaded("SF", "38")
                        repository.ensureTransitStopsLoaded("SF", "38R")
                        assertEquals("38", repository.getTransitStops("38").first().single().lineId)
                        assertEquals(
                            "38R",
                            repository.getTransitStops("38R").first().single().lineId,
                        )
                        assertTrue(local.isLoaded("stops:SF:38"))
                        assertTrue(local.isLoaded("stops:SF:38R"))
                    }
                assertEquals(listOf("SF"), lineCalls)
                assertEquals(listOf("38", "38R"), stopCalls)
            } finally {
                context.deleteDatabase(name)
            }
        }

    @Test
    fun migration_whenVersionTwoContainsData_preservesRecordsAndMarksPopulatedDatasets() =
        runBlocking {
            val name = "migration-${UUID.randomUUID()}"
            try {
                val configuration =
                    SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(name)
                        .callback(
                            object : SupportSQLiteOpenHelper.Callback(2) {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    db.execSQL(
                                        "CREATE TABLE transit_lines (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, fromDate TEXT NOT NULL, toDate TEXT NOT NULL, transportMode TEXT NOT NULL, publicCode TEXT NOT NULL, siriLineRef TEXT NOT NULL, monitored INTEGER NOT NULL, operatorRef TEXT NOT NULL)",
                                    )
                                    db.execSQL(
                                        "CREATE TABLE transit_stops (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, stopType TEXT, lineId TEXT NOT NULL, FOREIGN KEY(lineId) REFERENCES transit_lines(id) ON DELETE CASCADE)",
                                    )
                                    db.execSQL(
                                        "CREATE INDEX index_transit_stops_lineId ON transit_stops(lineId)",
                                    )
                                    db.execSQL(
                                        "INSERT INTO transit_lines VALUES ('38', 'Geary', '', '', 'bus', '38', '38', 1, 'SF')",
                                    )
                                    db.execSQL(
                                        "INSERT INTO transit_stops VALUES ('123', 'Market', 37.78, -122.41, NULL, '38')",
                                    )
                                }

                                override fun onUpgrade(
                                    db: SupportSQLiteDatabase,
                                    oldVersion: Int,
                                    newVersion: Int,
                                ) = Unit
                            },
                        )
                        .build()
                FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                    helper.writableDatabase
                }
                Room.databaseBuilder(context, TransitDatabase::class.java, name)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .useDatabase { database ->
                        val local = TransitLocalDataSourceImpl(database)
                        assertEquals("Geary", local.getTransitLines().first().single().name)
                        assertEquals("123", local.getStopsForLine("38").first().single().id)
                        assertTrue(local.isLoaded("lines:SF"))
                        assertTrue(local.isLoaded("stops:SF:38"))
                    }
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
