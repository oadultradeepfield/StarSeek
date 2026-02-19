package com.oadultradeepfield.starseek.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DispatcherModule {
  private const val MAX_BACKGROUND_TASKS = 5

  @Provides
  @Singleton
  @BackgroundDispatcher
  fun provideBackgroundDispatcher(): CoroutineDispatcher =
      Dispatchers.IO.limitedParallelism(MAX_BACKGROUND_TASKS)
}
