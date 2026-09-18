package com.nghianguyen.gomuni.data.datasource

import com.nghianguyen.gomuni.data.api.TransitApiService
import com.nghianguyen.gomuni.data.mapper.toDomain
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import javax.inject.Inject

class TransitRemoteDataSourceImpl @Inject constructor(
    private val apiService: TransitApiService
) : TransitRemoteDataSource {
    override suspend fun getTransitLines(operatorId: String): List<TransitLine> {
        return apiService.getTransitLines(operatorId).map { it.toDomain() }
    }

    override suspend fun getTransitStops(operatorId: String, lineId: String): List<TransitStop> {
        val response = apiService.getTransitStops(operatorId, lineId)
        return response.contents.dataObjects.scheduledStopPoints.map { it.toDomain(lineId) }
    }

    override suspend fun getBusArrivals(stopId: String): List<BusArrival> {
        val response = apiService.getStopMonitoring(
            stopCode = stopId
        )
        return response.serviceDelivery.stopMonitoringDelivery.monitoredStopVisit?.map {
            it.monitoredVehicleJourney.toDomain()
        } ?: emptyList()
    }
}
