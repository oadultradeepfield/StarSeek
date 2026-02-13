package com.oadultradeepfield.starseek.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.oadultradeepfield.starseek.image.ImageLoadingMetrics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
  private const val MEMORY_CACHE_PERCENT = 0.25
  private const val DISK_CACHE_SIZE = 50L * 1024 * 1024

  @Provides
  @Singleton
  fun provideImageLoader(
      @ApplicationContext context: Context,
      okHttpClient: OkHttpClient,
      metrics: ImageLoadingMetrics,
  ): ImageLoader =
      ImageLoader.Builder(context)
          .memoryCache {
            MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build()
          }
          .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_SIZE)
                .build()
          }
          .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
          .eventListener(metrics)
          .build()
}
