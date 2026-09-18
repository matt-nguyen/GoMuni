package com.nghianguyen.gomuni.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nghianguyen.gomuni.data.local.dao.TransitLineDao
import com.nghianguyen.gomuni.data.local.dao.TransitStopDao
import com.nghianguyen.gomuni.data.mapper.toDomain
import com.nghianguyen.gomuni.data.mapper.toEntity
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransitLocalDataSourceImpl @Inject constructor(
    private val transitLineDao: TransitLineDao,
    private val transitStopDao: TransitStopDao,
    private val dataStore: DataStore<Preferences>
) : TransitLocalDataSource {

    private object PreferencesKeys {
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    override fun getTransitLines(): Flow<List<TransitLine>> {
        return transitLineDao.getTransitLines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveTransitLines(lines: List<TransitLine>) {
        transitLineDao.insertAll(lines.map { it.toEntity() })
    }

    override suspend fun clearTransitLines() {
        transitLineDao.clearAll()
    }

    override fun getStopsForLine(lineId: String): Flow<List<TransitStop>> {
        return transitStopDao.getStopsForLine(lineId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveStopsForLine(lineId: String, stops: List<TransitStop>) {
        transitStopDao.insertAll(stops.map { it.toEntity() })
    }

    override suspend fun deleteStopsForLine(lineId: String) {
        transitStopDao.deleteStopsForLine(lineId)
    }

    override suspend fun getLastSyncTime(): Long {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L
    }

    override suspend fun saveLastSyncTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] = timestamp
        }
    }
}
