package com.nghianguyen.gomuni.data.local.dao

import androidx.room.*
import com.nghianguyen.gomuni.data.local.entity.TransitLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitLineDao {
    @Query("SELECT * FROM transit_lines") fun getTransitLines(): Flow<List<TransitLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<TransitLineEntity>): List<Long>

    @Query("DELETE FROM transit_lines") suspend fun clearAll(): Int
}
