package com.nghianguyen.gomuni.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve existing records and recognize populated datasets as already loaded. */
val MIGRATION_2_3 =
    object : Migration(2, INITIAL_LOAD_SCHEMA_VERSION) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE transit_stops_new (
                    id TEXT NOT NULL, name TEXT NOT NULL,
                    latitude REAL NOT NULL, longitude REAL NOT NULL,
                    stopType TEXT, lineId TEXT NOT NULL,
                    PRIMARY KEY(lineId, id),
                    FOREIGN KEY(lineId) REFERENCES transit_lines(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
                    .trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO transit_stops_new (id, name, latitude, longitude, stopType, lineId)
                SELECT id, name, latitude, longitude, stopType, lineId FROM transit_stops
                """
                    .trimIndent(),
            )
            db.execSQL("DROP TABLE transit_stops")
            db.execSQL("ALTER TABLE transit_stops_new RENAME TO transit_stops")
            db.execSQL("CREATE INDEX index_transit_stops_lineId ON transit_stops(lineId)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS loaded_datasets (dataset TEXT NOT NULL PRIMARY KEY)",
            )
            db.execSQL(
                "INSERT INTO loaded_datasets SELECT 'lines:SF' WHERE EXISTS (SELECT 1 FROM transit_lines)",
            )
            db.execSQL(
                "INSERT INTO loaded_datasets SELECT DISTINCT 'stops:SF:' || lineId FROM transit_stops",
            )
        }
    }

private const val INITIAL_LOAD_SCHEMA_VERSION = 3
