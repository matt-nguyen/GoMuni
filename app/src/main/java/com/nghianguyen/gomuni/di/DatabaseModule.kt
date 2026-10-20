package com.nghianguyen.gomuni.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nghianguyen.gomuni.data.local.MIGRATION_2_3
import com.nghianguyen.gomuni.data.local.TransitDatabase
import com.nghianguyen.gomuni.data.local.dao.TransitLineDao
import com.nghianguyen.gomuni.data.local.dao.TransitStopDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(name = "transit_preferences")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTransitDatabase(@ApplicationContext context: Context): TransitDatabase {
        return Room.databaseBuilder(
                context,
                TransitDatabase::class.java,
                "transit_database",
            )
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransitLineDao(database: TransitDatabase): TransitLineDao {
        return database.transitLineDao()
    }

    @Provides
    fun provideTransitStopDao(database: TransitDatabase): TransitStopDao {
        return database.transitStopDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
