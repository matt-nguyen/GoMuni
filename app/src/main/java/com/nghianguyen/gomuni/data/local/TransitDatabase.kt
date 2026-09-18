package com.nghianguyen.gomuni.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nghianguyen.gomuni.data.local.dao.TransitLineDao
import com.nghianguyen.gomuni.data.local.dao.TransitStopDao
import com.nghianguyen.gomuni.data.local.entity.TransitLineEntity
import com.nghianguyen.gomuni.data.local.entity.TransitStopEntity

@Database(
    entities = [TransitLineEntity::class, TransitStopEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TransitDatabase : RoomDatabase() {
    abstract fun transitLineDao(): TransitLineDao
    abstract fun transitStopDao(): TransitStopDao
}
