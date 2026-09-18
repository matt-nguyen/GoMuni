package com.nghianguyen.gomuni.data.remote.model

import com.google.gson.annotations.SerializedName

data class TransitLineDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("FromDate") val fromDate: String,
    @SerializedName("ToDate") val toDate: String,
    @SerializedName("TransportMode") val transportMode: String,
    @SerializedName("PublicCode") val publicCode: String,
    @SerializedName("SiriLineRef") val siriLineRef: String,
    @SerializedName("Monitored") val monitored: Boolean,
    @SerializedName("OperatorRef") val operatorRef: String
)
