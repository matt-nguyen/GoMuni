package com.nghianguyen.gomuni.data.datasource

import androidx.room.withTransaction
import com.nghianguyen.gomuni.data.local.TransitDatabase
import com.nghianguyen.gomuni.data.local.entity.TransitLineStopEntity
import com.nghianguyen.gomuni.data.mapper.toDomain
import com.nghianguyen.gomuni.data.mapper.toEntity
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class TransitLocalDataSourceImpl @Inject constructor(private val database: TransitDatabase) :
    TransitLocalDataSource {
    override fun getTransitLines() =
        database.transitLineDao().getTransitLines().map { lines -> lines.map { it.toDomain() } }

    override fun getStopsForLine(lineId: String) =
        database.transitStopDao().getStopsForLine(lineId).map { stops ->
            stops.map { it.toDomain(lineId) }
        }

    override suspend fun storeInitialLines(operatorId: String, lines: List<TransitLine>) {
        database.withTransaction {
            database.transitLineDao().insertAll(lines.map { it.toEntity() })
        }
    }

    override suspend fun storeInitialStops(
        operatorId: String,
        lineId: String,
        stops: List<TransitStop>,
    ) {
        database.withTransaction {
            database.transitStopDao().insertAll(stops.map { it.toEntity() })
            database
                .transitStopDao()
                .insertAssociations(stops.map { TransitLineStopEntity(lineId, it.id) })
        }
    }
}
