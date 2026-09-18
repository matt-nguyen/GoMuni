package com.nghianguyen.gomuni.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nghianguyen.gomuni.data.local.entity.TransitStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitStopDao {
    @Query("SELECT * FROM transit_stops WHERE lineId = :lineId")
    fun getStopsForLine(lineId: String): Flow<List<TransitStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<TransitStopEntity>): List<Long>

    @Query("DELETE FROM transit_stops WHERE lineId = :lineId")
    suspend fun deleteStopsForLine(lineId: String): Int
}
