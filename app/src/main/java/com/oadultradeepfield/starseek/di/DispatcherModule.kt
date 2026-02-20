package com.oadultradeepfield.starseek.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DispatcherModule {
  @Provides
  @Singleton
  @BackgroundDispatcher
  fun provideBackgroundDispatcher(appExecutors: AppExecutors): CoroutineDispatcher =
      appExecutors.backgroundExecutor.asCoroutineDispatcher()
}
