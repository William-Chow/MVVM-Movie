package com.mvvmmovie.di

import com.mvvmmovie.network.AndroidNetworkMonitor
import com.mvvmmovie.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(monitor: AndroidNetworkMonitor): NetworkMonitor
}
