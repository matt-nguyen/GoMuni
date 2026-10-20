package com.nghianguyen.gomuni.di

import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSource
import com.nghianguyen.gomuni.data.datasource.TransitLocalDataSourceImpl
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSource
import com.nghianguyen.gomuni.data.datasource.TransitRemoteDataSourceImpl
import com.nghianguyen.gomuni.data.repository.TransitRepositoryImpl
import com.nghianguyen.gomuni.data.util.AnalyticsErrorLogger
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import com.nghianguyen.gomuni.domain.util.ErrorLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    @Singleton
    fun bindTransitRemoteDataSource(
        transitRemoteDataSourceImpl: TransitRemoteDataSourceImpl,
    ): TransitRemoteDataSource

    @Binds
    @Singleton
    fun bindTransitLocalDataSource(
        transitLocalDataSourceImpl: TransitLocalDataSourceImpl,
    ): TransitLocalDataSource

    @Binds
    @Singleton
    fun bindTransitRepository(
        transitRepositoryImpl: TransitRepositoryImpl,
    ): TransitRepository

    @Binds
    @Singleton
    fun bindErrorLogger(
        analyticsErrorLogger: AnalyticsErrorLogger,
    ): ErrorLogger
}
