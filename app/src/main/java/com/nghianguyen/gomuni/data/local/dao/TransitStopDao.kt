package com.nghianguyen.gomuni.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.nghianguyen.gomuni.data.local.entity.TransitLineStopEntity
import com.nghianguyen.gomuni.data.local.entity.TransitStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitStopDao {
    @Query(
        "SELECT s.* FROM transit_stops s " +
            "INNER JOIN transit_line_stops ls ON s.id = ls.stopId WHERE ls.lineId = :lineId",
    )
    fun getStopsForLine(lineId: String): Flow<List<TransitStopEntity>>

    @Upsert suspend fun insertAll(stops: List<TransitStopEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssociations(associations: List<TransitLineStopEntity>)
}
