package com.nghianguyen.gomuni.data.remote.model

import com.google.gson.annotations.SerializedName

data class StopMonitoringResponse(
    @SerializedName("ServiceDelivery")
    val serviceDelivery: ServiceDelivery
)

data class ServiceDelivery(
    @SerializedName("StopMonitoringDelivery")
    val stopMonitoringDelivery: StopMonitoringDelivery
)

data class StopMonitoringDelivery(
    @SerializedName("MonitoredStopVisit")
    val monitoredStopVisit: List<MonitoredStopVisit>?
)

data class MonitoredStopVisit(
    @SerializedName("MonitoredVehicleJourney")
    val monitoredVehicleJourney: MonitoredVehicleJourney
)

data class MonitoredVehicleJourney(
    @SerializedName("LineRef")
    val lineRef: String,
    @SerializedName("PublishedLineName")
    val publishedLineName: String,
    @SerializedName("VehicleLocation")
    val vehicleLocation: VehicleLocation,
    @SerializedName("VehicleRef")
    val vehicleRef: String?,
    @SerializedName("MonitoredCall")
    val monitoredCall: MonitoredCall
)

data class VehicleLocation(
    @SerializedName("Longitude")
    val longitude: String,
    @SerializedName("Latitude")
    val latitude: String
)

data class MonitoredCall(
    @SerializedName("ExpectedArrivalTime")
    val expectedArrivalTime: String?,
    @SerializedName("AimedArrivalTime")
    val aimedArrivalTime: String?
)
