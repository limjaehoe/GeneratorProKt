package com.androidkotlin.generatorprokt.di

import com.androidkotlin.generatorprokt.data.repository.Serial422RepositoryImpl
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SerialModule {

    @Binds
    @Singleton
    abstract fun bindSerial422Repository(
        repositoryImpl: Serial422RepositoryImpl
    ): Serial422Repository
}