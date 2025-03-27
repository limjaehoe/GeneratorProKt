package com.androidkotlin.generatorprokt.di

import com.androidkotlin.generatorprokt.data.device.DeviceDataSource
import com.androidkotlin.generatorprokt.data.repository.DeviceDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceModule {

    @Binds
    @Singleton
    abstract fun bindDeviceDataSource(
        deviceDataSourceImpl: DeviceDataSourceImpl
    ): DeviceDataSource
}