package com.oadultradeepfield.starseek.di

import android.content.Context
import androidx.room.Room
import com.oadultradeepfield.starseek.BuildConfig
import com.oadultradeepfield.starseek.data.local.ObjectDetailDao
import com.oadultradeepfield.starseek.data.local.SolveDao
import com.oadultradeepfield.starseek.data.local.StarSeekDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): StarSeekDatabase =
      Room.databaseBuilder(context, StarSeekDatabase::class.java, "starseek_database")
          .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration(false) }
          .build()

  @Provides
  @Singleton
  fun provideSolveDao(database: StarSeekDatabase): SolveDao = database.solveDao()

  @Provides
  @Singleton
  fun provideObjectDetailDao(database: StarSeekDatabase): ObjectDetailDao =
      database.objectDetailDao()
}
