package com.nghianguyen.gomuni.data.api

import com.nghianguyen.gomuni.data.remote.model.StopMonitoringResponse
import com.nghianguyen.gomuni.data.remote.model.TransitLineDto
import com.nghianguyen.gomuni.data.remote.model.TransitStopResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TransitApiService {
    @GET("transit/lines")
    suspend fun getTransitLines(
        @Query("operator_id") operatorId: String
    ): List<TransitLineDto>

    @GET("transit/stops")
    suspend fun getTransitStops(
        @Query("operator_id") operatorId: String,
        @Query("line_id") lineId: String
    ): TransitStopResponse

    @GET("transit/StopMonitoring")
    suspend fun getStopMonitoring(
        @Query("stopCode") stopCode: String,
        @Query("agency") agency: String = "SF"
    ): StopMonitoringResponse
}
