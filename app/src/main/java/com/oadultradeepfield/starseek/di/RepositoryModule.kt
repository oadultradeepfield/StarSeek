package com.oadultradeepfield.starseek.di

import com.oadultradeepfield.starseek.data.repository.SolveRepository
import com.oadultradeepfield.starseek.data.repository.SolveRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds @Singleton abstract fun bindSolveRepository(impl: SolveRepositoryImpl): SolveRepository
}
