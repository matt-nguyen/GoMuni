package com.nghianguyen.gomuni.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transit_stops",
    primaryKeys = ["lineId", "id"],
    foreignKeys =
        [
            ForeignKey(
                entity = TransitLineEntity::class,
                parentColumns = ["id"],
                childColumns = ["lineId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index(value = ["lineId"])],
)
data class TransitStopEntity(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val stopType: String?,
    val lineId: String,
)
