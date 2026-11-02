package com.nghianguyen.gomuni.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transit_line_stops",
    primaryKeys = ["lineId", "stopId"],
    foreignKeys =
        [
            ForeignKey(
                entity = TransitLineEntity::class,
                parentColumns = ["id"],
                childColumns = ["lineId"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = TransitStopEntity::class,
                parentColumns = ["id"],
                childColumns = ["stopId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index(value = ["stopId"])],
)
data class TransitLineStopEntity(val lineId: String, val stopId: String)
