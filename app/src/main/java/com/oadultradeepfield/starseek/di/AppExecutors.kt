package com.oadultradeepfield.starseek.di

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppExecutors @Inject constructor() {
  val backgroundExecutor: ExecutorService =
      Executors.newFixedThreadPool(BACKGROUND_POOL_SIZE) { runnable ->
        Thread(runnable, "starseek-bg-${threadCounter.getAndIncrement()}")
      }

  companion object {
    private const val BACKGROUND_POOL_SIZE = 4
    private val threadCounter = AtomicInteger(0)
  }
}
