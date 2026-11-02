package com.nghianguyen.gomuni.data.datasource

import com.nghianguyen.gomuni.data.local.dao.TransitLineDao
import com.nghianguyen.gomuni.data.local.dao.TransitStopDao
import com.nghianguyen.gomuni.data.local.entity.TransitLineStopEntity
import com.nghianguyen.gomuni.data.mapper.toDomain
import com.nghianguyen.gomuni.data.mapper.toEntity
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class TransitLocalDataSourceImpl
@Inject
constructor(
    private val transitLineDao: TransitLineDao,
    private val transitStopDao: TransitStopDao,
) : TransitLocalDataSource {
    override fun getTransitLines() =
        transitLineDao.getTransitLines().map { lines -> lines.map { it.toDomain() } }

    override fun getStopsForLine(lineId: String) =
        transitStopDao.getStopsForLine(lineId).map { stops ->
            stops.map { it.toDomain(lineId) }
        }

    override suspend fun storeInitialLines(operatorId: String, lines: List<TransitLine>) {
        transitLineDao.insertAll(lines.map { it.toEntity() })
    }

    override suspend fun storeInitialStops(
        operatorId: String,
        lineId: String,
        stops: List<TransitStop>,
    ) {
        transitStopDao.insertStopsWithAssociations(
            stops.map { it.toEntity() },
            stops.map { TransitLineStopEntity(lineId, it.id) },
        )
    }
}
