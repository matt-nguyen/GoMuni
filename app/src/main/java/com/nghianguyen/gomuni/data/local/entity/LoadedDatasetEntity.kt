package com.nghianguyen.gomuni.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records completed initial loads, including successful empty responses. */
@Entity(tableName = "loaded_datasets")
data class LoadedDatasetEntity(@PrimaryKey val dataset: String)
