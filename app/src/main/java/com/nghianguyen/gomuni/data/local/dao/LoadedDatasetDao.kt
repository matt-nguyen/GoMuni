package com.nghianguyen.gomuni.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nghianguyen.gomuni.data.local.entity.LoadedDatasetEntity

@Dao
interface LoadedDatasetDao {
    @Query("SELECT EXISTS(SELECT 1 FROM loaded_datasets WHERE dataset = :dataset)")
    suspend fun isLoaded(dataset: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markLoaded(dataset: LoadedDatasetEntity)
}
