package com.nghianguyen.gomuni.data.remote.model

import com.google.gson.annotations.SerializedName

data class TransitStopResponse(
    @SerializedName("Contents") val contents: ContentsDto
)

data class ContentsDto(
    @SerializedName("ResponseTimestamp") val responseTimestamp: String,
    @SerializedName("dataObjects") val dataObjects: DataObjectsDto
)

data class DataObjectsDto(
    @SerializedName("id") val id: String,
    @SerializedName("ScheduledStopPoint") val scheduledStopPoints: List<ScheduledStopPointDto>
)

data class ScheduledStopPointDto(
    @SerializedName("id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Location") val location: LocationDto,
    @SerializedName("Url") val url: String?,
    @SerializedName("StopType") val stopType: String?,
    @SerializedName("Extensions") val extensions: ExtensionsDto?
)

data class LocationDto(
    @SerializedName("Longitude") val longitude: String,
    @SerializedName("Latitude") val latitude: String
)

data class ExtensionsDto(
    @SerializedName("LocationType") val locationType: String?,
    @SerializedName("PlatformCode") val platformCode: String?,
    @SerializedName("ParentStation") val parentStation: String?,
    @SerializedName("ValidBetween") val validBetween: ValidBetweenDto?
)

data class ValidBetweenDto(
    @SerializedName("FromDate") val fromDate: String,
    @SerializedName("ToDate") val toDate: String
)
