package com.androidkotlin.generatorprokt.di

import com.androidkotlin.generatorprokt.data.repository.GeneratorRepositoryImpl
import com.androidkotlin.generatorprokt.domain.repository.GeneratorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GeneratorModule {

    @Binds
    @Singleton
    abstract fun bindGeneratorRepository(
        generatorRepositoryImpl: GeneratorRepositoryImpl
    ): GeneratorRepository
}