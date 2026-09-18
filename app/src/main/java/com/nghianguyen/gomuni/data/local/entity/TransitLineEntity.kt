package com.nghianguyen.gomuni.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transit_lines")
data class TransitLineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fromDate: String,
    val toDate: String,
    val transportMode: String,
    val publicCode: String,
    val siriLineRef: String,
    val monitored: Boolean,
    val operatorRef: String
)
