package com.nghianguyen.gomuni.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nghianguyen.gomuni.data.local.dao.LoadedDatasetDao
import com.nghianguyen.gomuni.data.local.dao.TransitLineDao
import com.nghianguyen.gomuni.data.local.dao.TransitStopDao
import com.nghianguyen.gomuni.data.local.entity.LoadedDatasetEntity
import com.nghianguyen.gomuni.data.local.entity.TransitLineEntity
import com.nghianguyen.gomuni.data.local.entity.TransitStopEntity

@Database(
    entities = [TransitLineEntity::class, TransitStopEntity::class, LoadedDatasetEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class TransitDatabase : RoomDatabase() {
    abstract fun loadedDatasetDao(): LoadedDatasetDao

    abstract fun transitLineDao(): TransitLineDao

    abstract fun transitStopDao(): TransitStopDao
}
