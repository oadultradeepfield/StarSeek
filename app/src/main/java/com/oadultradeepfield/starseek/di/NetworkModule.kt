package com.oadultradeepfield.starseek.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.oadultradeepfield.starseek.BuildConfig
import com.oadultradeepfield.starseek.data.remote.StarSeekApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides @Singleton fun provideJson(): Json = Json { ignoreUnknownKeys = true }

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(
              HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
              }
          )
          .build()

  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
      Retrofit.Builder()
          .baseUrl(BuildConfig.API_BASE_URL)
          .client(okHttpClient)
          .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
          .build()

  @Provides
  @Singleton
  fun provideStarSeekApi(retrofit: Retrofit): StarSeekApi = retrofit.create(StarSeekApi::class.java)
}
